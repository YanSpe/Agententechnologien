package de.dailab.jiacvi.aot.auction

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import java.time.Duration
import kotlin.random.Random


// topic where to send messages for all bidders
const val biddersTopic = "all-bidders"

// name/id by which to resolve the auctioneer agent
const val auctioneer = "auctioneer"

/**
 * Agent for running the auction and evaluating bids. After inviting all bidders to join the auction,
 * the auctioneer will accept Offers and Bids for those offers. In regular intervals, it broadcasts
 * new offers to all bidders and evaluates the Bids for the previous round of Offers. Finally, after
 * a set number of total turns or since the last offer, the auctioneer closes the auction.
 *
 * The auctioneer does _not_ check whether the bidder agents actually have the items they offer, or
 * the credits to pay for their bids during the auction, but if they have a "debt" at the end, their
 * score will be zero.
 */
class AuctioneerAgent(
        private val maxTurns: Int,
        private val maxTurnsNoOffer: Int,
        private val turnSeconds: Int
    ): Agent(overrideName=auctioneer) {

    enum class Phase {
        REGISTERING, STARTING, BIDDING, EVALUATING, END
    }

    private val broker by resolve<BrokerAgentRef>()

    private var phase = Phase.REGISTERING

    private var turn = 0

    private var lastOfferTurn = 0

    // random secret numbers used for each bidder for very simple authentication in both sides
    private val secrets = mutableMapOf<String, Int>()

    // for keeping track of bidder agents' wallets (they should do the same)
    private val wallets = mutableMapOf<String, Wallet>()

    // current "pools" of items to change their owner, grouped by item type
    private val pools = mutableMapOf<Item, MutableList<Offer>>()


    override fun behaviour() = act {

        // regular behaviour executed each turn, depending on current phase, defined below
        every(Duration.ofSeconds(turnSeconds.toLong())) {
            when (phase) {
                Phase.REGISTERING -> registerPhase()
                Phase.STARTING -> startPhase()
                Phase.BIDDING -> biddingPhase()
                Phase.EVALUATING -> evaluationPhase()
                Phase.END -> {}
            }
        }

        // handle and respond to Register messages
        respond<Register, Boolean> {
            if (it.bidderId in secrets || phase != Phase.STARTING) {
                false
            } else {
                secrets[it.bidderId] = Random.nextInt()
                log.info("Registered Bidder $it.bidderId, Secret ${secrets[it.bidderId]}")
                true
            }
        }

        // handle and respond to new Offers
        respond<Offer, Boolean> {
            if (secrets[it.bidderId] == it.secret && it.bid >= 0) {
                log.info("Received valid Offer: $it")

                // check current pending Offers of this bidder to ensure they have the credits
                // (they do not need to actually have the item, resulting in a short sale if they don't buy it)
                val pending = pools.values.flatten().filter { x -> x.bidderId == it.bidderId }
                    .sumByDouble { x -> x.bid }

                if (wallets[it.bidderId]!!.credits >= pending + it.bid) {
                    pools.putIfAbsent(it.item, mutableListOf())
                    pools[it.item]!!.add(it)
                    true
                } else {
                    false
                }
            } else {
                log.info("Received invalid Offer: $it")
                false
            }
        }

        respond<CashIn, CashInResult> {
            // check that the Bidder currently has there Items in its Wallet
            // (they may at the same time offer those items; those would then result in short sales)
            val held = wallets[it.bidderId]!!.items[it.item] ?: 0
            if (secrets[it.bidderId] == it.secret && it.num > 0 && held >= it.num) {
                val value = fibsum(it.num).toDouble()
                wallets[it.bidderId]!!.update(it.item, -it.num, +value)
                CashInResult(value)
            } else {
                CashInResult(0.0)
            }
        }
    }

    private fun registerPhase() {
        val message = StartAuction(maxTurns, maxTurnsNoOffer, turnSeconds)
        log.info("Sending Start message to '$biddersTopic' topic")
        broker.publish(biddersTopic, message)

        phase = Phase.STARTING
    }

    /**
     * start the auction, sending a message to all registered bidders
     */
    private fun startPhase() {
        val numBidders = secrets.size
        val credits = 500.0
        // create initial distribution of items. Each bidder has overall the same amount but different item types
        val itemDist = (1..numBidders).map { (0..5).random() }

        for ((bidderId, secret) in secrets.entries) {
            // shuffle the item types for each bidder
            val items = itemDist.shuffled().mapIndexed { i: Int, amount: Int ->
                Item(i+1) to amount }.toMap()
            wallets[bidderId] = Wallet(bidderId, items.toMutableMap(), credits)

            val ref = system.resolve(bidderId)
            ref tell Registered(secret, numBidders, items, credits)
        }

        phase = Phase.BIDDING
    }

    /**
     * during the bidding phase, first get all already advertised offers (where bid-lists exist),
     * sort the bids in descending order, determine the final price, and send messages to both
     * seller and buyer (if any); then advertise any new Offers to the bidder agents
     */
    private fun biddingPhase() {
        turn++
        log.info("Bidding in Turn $turn, (${pools.values.sumBy { it.size }} open offers)")

        val itemStats = mutableMapOf<Item, Stats>()

        for ((item, offers) in pools.entries.toList()) {
            pools.remove(item)
            if (offers.isEmpty()) continue

            lastOfferTurn = turn

            offers.sortBy { it.bid }
            val bids = offers.map { it.bid }
            log.info("Offers for $item: $bids")

            val price = median(bids)
            val middle: Double = (offers.size - 1) / 2.0
            for ((i, offer) in offers.withIndex()) {

                // determine if item was sold, bought, or no transfer
                val transfer = when (i.toDouble().compareTo(middle)) {
                    -1 -> Transfer.SOLD
                    +1 -> Transfer.BOUGHT
                    else -> Transfer.NONE
                }

                // send result to bidder
                val result = OfferResult(secrets[offer.bidderId]!!, item, transfer, price)
                log.info("Result for $offer: $result")
                val ref = system.resolve(offer.bidderId)
                ref tell result

                // update wallet
                when (transfer) {
                    Transfer.SOLD   -> wallets[offer.bidderId]!!.update(item, -1, +price)
                    Transfer.BOUGHT -> wallets[offer.bidderId]!!.update(item, +1, -price)
                    else -> {}
                }
            }
            itemStats[item] = Stats(offers.size, bids.min()!!, price, bids.max()!!)
        }

        // send digest
        log.info("Sending Digest to '$biddersTopic' topic")
        broker.publish(biddersTopic, Digest(turn, itemStats.toMap()))

        // check that after each round, total items & credits remain the same (except after cash-in)
        log.debug("TOTAL ITEMS: ${wallets.values.flatMap { it.items.values }.sum()}")
        log.debug("TOTAL MONEY: ${wallets.values.sumByDouble { it.credits }}")

        // switch phase?
        if (turn > maxTurns || turn - lastOfferTurn > maxTurnsNoOffer) {
            phase = Phase.EVALUATING
        }
    }

    /**
     * determine value of final wallets and send messages to all registered bidders
     */
    private fun evaluationPhase() {
        log.info("Closing Auction...")
        wallets.values.sortedByDescending { it.value() }.forEachIndexed { i, it ->
            val ref = system.resolve(it.bidderId)
            ref tell AuctionResult(it.value(), i+1, it.items.toMap(), it.credits)
        }

        phase = Phase.END
    }

}
