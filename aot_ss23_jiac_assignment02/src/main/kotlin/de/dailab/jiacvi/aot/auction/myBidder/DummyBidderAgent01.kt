package de.dailab.jiacvi.aot.auction.myBidder

import de.dailab.jiacvi.aot.auction.*
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import kotlin.system.exitProcess

/**
 * Problem --> der hier ist schlechter, als der andere --> warum???
 * bekommen auch immer negative items --> warum???
 * --> schicken zu viele Offers --> verdoppeln pro Runde
 */
class DummyBidderAgent01(private val id: String) : Agent(overrideName = id) {
    // you can use the broker to broadcast messages i.e. broker.publish(biddersTopic, LookingFor(...))
    private val broker by resolve<BrokerAgentRef>()
    private var itemStats: Map<Item, Stats>? = null
    // keep track of the bidder agent's own wallet
    private var wallet: Wallet? = null
    private var secret: Int = -1
    private var epsilon: Double = 0.01

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
            itemStats = it.itemStats
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

    private fun maxPrice(number: Int): Double {
        return (fib(number + 1).toDouble())
    }

    private fun minValue(number: Int):Double {
        return fib(number).toDouble()
    }

    private fun getPrice(item:  MutableMap.MutableEntry<Item, Int>): Double? {
        //ich möchte den Gewinn maximieren, egal ob per Geld oder Items. Niemals ins negative gehen
        if (itemStats != null && !itemStats!!.isEmpty()) {
            val stats = itemStats!!.get(item.key)
            val maxPrice = maxPrice(item.value)
            val minValue = minValue(item.value)

            if (maxPrice > stats!!.median) {
                // kaufen --> Median möchte ich senken, aber noch das Item bekommen
                val newPrice = stats.median + epsilon
                return if (newPrice >= minValue) newPrice
                else null
            } else if (maxPrice == stats.median) {
                //mache auf alle Fälle Gewinn >= 0
                //verkaufen --> median > maxprice > minValue --> Gewinn: median - minValue
                // kaufen --> median < maxPrice --> Gewinn: maxPrice - median
                //median == maxPrice -> Gewinn = 0
                return if (maxPrice >= minValue) maxPrice
                else null
            } else {
                //verkaufen --> Median erhöhen, aber noch das Item verkaufen
                val newPrice = stats.median - epsilon
                return if (newPrice >= minValue) newPrice
                else null
            }
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