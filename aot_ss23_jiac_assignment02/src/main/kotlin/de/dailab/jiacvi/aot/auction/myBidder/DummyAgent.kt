package de.dailab.jiacvi.aot.auction.myBidder

import de.dailab.jiacvi.aot.auction.*
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import kotlin.system.exitProcess

/**
 * This is a simple stub of the Bidder Agent. You can use this as a template to start your implementation.
 */
class DummyAgent(private val id: String) : Agent(overrideName = id) {
    // you can use the broker to broadcast messages i.e. broker.publish(biddersTopic, LookingFor(...))
    private val broker by resolve<BrokerAgentRef>()

    // keep track of the bidder agent's own wallet
    private var wallet: Wallet? = null
    private var secret: Int = -1

    override fun behaviour() = act {
        // easy - Bidder Agent.
        // buy and sell only received goods with formula: fib(number of good+1) - fib(number of good)

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

    private fun bid() {
        val ref = system.resolve(auctioneer)
        if (wallet != null) {
            for (item in wallet!!.items) {
                if (item.value == 0) continue
                val optimalPrice = fib(item.value + 1) - fib(item.value)
                //val optimalPrice = fib(item.value)
                if (optimalPrice <= wallet!!.credits) {
                    ref invoke ask<Boolean>(Offer(id, secret, item.key, optimalPrice.toDouble())) { res ->
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