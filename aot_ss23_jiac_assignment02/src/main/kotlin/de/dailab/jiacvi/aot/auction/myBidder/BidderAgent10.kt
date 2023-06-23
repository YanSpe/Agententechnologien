package de.dailab.jiacvi.aot.auction.myBidder

import de.dailab.jiacvi.aot.auction.*
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import kotlin.system.exitProcess

class BidderAgent10(private val id: String) : Agent(overrideName = id) {
    // you can use the broker to broadcast messages i.e. broker.publish(biddersTopic, LookingFor(...))
    private val broker by resolve<BrokerAgentRef>()
    private var itemStats: ArrayList<Map<Item, Stats>> = ArrayList()
    // keep track of the bidder agent's own wallet
    private var wallet: Wallet? = null
    private var secret: Int = -1

    override fun behaviour() = act {

        // register to all started auctions
        listen<StartAuction>(biddersTopic) {
            val message = Register(id)
            log.info("Received $it, Sending $message")

            val ref = system.resolve(auctioneer)
            ref invoke ask<Boolean>(message) { res ->
                log.info("Registered: $res")
            }
        }

        // handle Registered message, initialize Wallet
        on<Registered> {
            wallet = Wallet(id, it.items.toMutableMap(), it.credits)
            secret = it.secret
            log.info("Initialized Wallet: $wallet, secret: $secret")
            bid()
        }

        // be notified of result of own offer
        on<OfferResult> {
            log.info("Result for my Offer: $it")
            when (it.transfer) {
                Transfer.SOLD -> wallet?.update(it.item, -1, +it.price)
                Transfer.BOUGHT -> wallet?.update(it.item, +1, -it.price)
                else -> {}
            }

        }

        listen<Digest>(biddersTopic) {
            log.debug("Received Digest: $it")
            itemStats.add(it.itemStats)
            log.info(itemStats.size.toString())
            bid()
        }

        listen<LookingFor>(biddersTopic) {
            log.debug("Received LookingFor: $it")
        }

        // be notified of result of the entire auction
        on<AuctionResult> {
            log.info("Result of Auction: $it")
            wallet = null
            exitProcess(0)
        }

    }

    private fun getMedian(item: Item): Double {
        var sum = 0.0
        for (digest in itemStats) {
            sum += digest.get(item)?.median ?: 0.0
        }
        return (sum / itemStats.size)
    }

    private fun getVarianz(item: Item, median: Double): Double {
        var varianz = 0.0
        //log.info("varianz aus " + "median: " + median + " und stats: " + itemStats + " für item: " + item)
        for (digest in itemStats) {
            val newMedian = digest.get(item)?.median
            if (newMedian == null) continue
            val pow = Math.pow((newMedian - median), 2.0)
            varianz += pow * (1/itemStats.size)
        }
        return varianz
    }

    private fun maxPrice(number: Int): Double {
        return (fib(number + 1).toDouble())
    }

    private fun minValue(number: Int):Double {
        return fib(number).toDouble()
    }

    private fun getPrice(item:  MutableMap.MutableEntry<Item, Int>): Double? {
        //ich möchte den Gewinn maximieren, egal ob per Geld oder Items. Niemals ins negative gehen
        if (!itemStats.isEmpty()) {
            val median = getMedian(item.key)
            val maxPrice = maxPrice(item.value)
            val minValue = minValue(item.value)
            val varianz = getVarianz(item.key, median)

            return maxPrice / (median + varianz)
        } else {
            return maxPrice(item.value)
        }
    }

    private fun bid() {
        val ref = system.resolve(auctioneer)
        if (wallet != null) {
            for (item in wallet!!.items) {
                if (item.value == 0) continue
                val optimalPrice = getPrice(item)
                if (optimalPrice != null && optimalPrice <= wallet!!.credits) {
                    ref invoke ask<Boolean>(Offer(id, secret, item.key, optimalPrice)) { res ->
                    }
                }
            }
        }
    }

    private fun getItemOfMinimalNumber(): Item? {
        if (wallet != null) {
            var maxItem = Item(-1)
            var maxValue = -1
            for (item in wallet!!.items) {
                if (item.value > maxValue) {
                    maxItem = item.key
                    maxValue = item.value
                }
            }
            return maxItem
        }
        return null
    }

}