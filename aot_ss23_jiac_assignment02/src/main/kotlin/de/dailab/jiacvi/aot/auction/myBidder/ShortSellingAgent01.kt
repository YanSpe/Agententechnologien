package de.dailab.jiacvi.aot.auction.myBidder

import de.dailab.jiacvi.aot.auction.*
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import kotlin.math.pow
import kotlin.system.exitProcess

class ShortSellingAgent01(private val id: String) : Agent(overrideName = id) {
    // you can use the broker to broadcast messages i.e. broker.publish(biddersTopic, LookingFor(...))
    private val broker by resolve<BrokerAgentRef>()
    private var itemStats: ArrayList<Map<Item, Stats>> = ArrayList()
    // keep track of the bidder agent's own wallet
    private var wallet: Wallet? = null
    private var secret: Int = -1
    private var lookingFors: Array<ArrayList<Price>> = Array<ArrayList<Price>>(100) { ArrayList<Price>() }

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

        // save digest
        listen<Digest>(biddersTopic) {
            log.debug("Received Digest: $it")
            itemStats.add(it.itemStats)
            log.info(itemStats.size.toString())
            lookingFor()
            bid()
        }

        // save LookingFors
        listen<LookingFor>(biddersTopic) {
            log.debug("Received LookingFor: $it")
            lookingFors[it.item.type].add(it.price)
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
        for (p in lookingFors[item.type]) {
            sum += p
        }
        return (sum / (itemStats.size + lookingFors[item.type].size))
    }

    private fun getVarianz(item: Item, median: Double): Double {
        var varianz = 0.0
        //log.info("varianz aus " + "median: " + median + " und stats: " + itemStats + " für item: " + item)
        for (digest in itemStats) {
            val newMedian = digest[item]?.median
            if (newMedian == null) continue
            val pow = (newMedian - median).pow(2.0)
            varianz += pow * (1/itemStats.size)
        }
        return varianz
    }

    private fun maxPrice(number: Int): Double {
        return (fib(number + 1).toDouble())
    }

    private fun minPrice(number: Int):Double {
        return fib(number).toDouble()
    }

    private fun getPrice(item:  MutableMap.MutableEntry<Item, Int>): Double? {
        //ich möchte den Gewinn maximieren, egal ob per Geld oder Items. Niemals ins negative gehen
        if (!itemStats.isEmpty()) {
            val minPrice = minPrice(item.value)
            val maxPrice = maxPrice(item.value)
            val median = getMedian(item.key)
            val varianz = getVarianz(item.key, median)

            if (maxPrice > median) {
                // kaufen --> Median möchte ich senken, aber noch das Item bekommen
                val newPrice = median + varianz
                return if (newPrice >= minPrice) newPrice
                else null

            } else if (maxPrice == median) {
                //mache auf alle Fälle Gewinn >= 0
                //verkaufen --> median > maxprice > minValue --> Gewinn: median - minValue
                // kaufen --> median < maxPrice --> Gewinn: maxPrice - median
                //median == maxPrice -> Gewinn = 0
                return if (maxPrice >= minPrice) maxPrice
                else null

            } else {
                //verkaufen --> Median erhöhen, aber noch das Item verkaufen
                val newPrice = median - varianz
                return if (newPrice >= minPrice) newPrice
                else null
            }
        } else {
            return maxPrice(item.value)
        }
    }

    private fun maxShortPrice(number: Int): Double {
        if (number >= 0){
            return (fib(number + 1).toDouble())
        } else {
            return ((fib(-number)*2).toDouble())
        }
    }

    private fun minShortPrice(number: Int):Double {
        return ((fib(-(number-1))*2).toDouble())
    }

    private fun getShortPrice(item:  MutableMap.MutableEntry<Item, Int>): Double? {
        //ich möchte den Gewinn maximieren, egal ob per Geld oder Items. Niemals ins negative gehen
        if (!itemStats.isEmpty()) {
            val minPrice = minShortPrice(item.value)
            val maxPrice = maxShortPrice(item.value)
            val median = getMedian(item.key)
            val varianz = getVarianz(item.key, median)

            if (maxPrice > median) {
                // kaufen --> Median möchte ich senken, aber noch das Item bekommen
                val newPrice = median + varianz
                return if (newPrice >= minPrice) newPrice
                else null

            } else if (maxPrice == median) {
                //mache auf alle Fälle Gewinn >= 0
                //verkaufen --> median > maxprice > minValue --> Gewinn: median - minValue
                // kaufen --> median < maxPrice --> Gewinn: maxPrice - median
                //median == maxPrice -> Gewinn = 0
                return if (maxPrice >= minPrice) maxPrice
                else null

            } else {
                //verkaufen --> Median erhöhen, aber noch das Item verkaufen
                val newPrice = median - varianz
                return if (newPrice >= minPrice) newPrice
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
                if (item.value <= 0) {
                    val optimalPrice = getShortPrice(item)
                    if (optimalPrice != null && optimalPrice <= wallet!!.credits) {
                        ref invoke ask<Boolean>(Offer(id, secret, item.key, optimalPrice)) { res ->
                        }
                    }
                } else {
                    val optimalPrice = getPrice(item)
                    if (optimalPrice != null && optimalPrice <= wallet!!.credits) {
                        ref invoke ask<Boolean>(Offer(id, secret, item.key, optimalPrice)) { res ->
                        }
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

    private fun lookingFor() {
        // find Item with most value
        if (wallet != null) {
            var maxItem = Item(-1)
            var maxValue = -1
            for (item in wallet!!.items) {
                if (item.value > maxValue) {
                    maxItem = item.key
                    maxValue = item.value
                }
            }
            var i = maxPrice(maxValue)
            // send LookingFor
            log.info("Sending LookingFor to '$biddersTopic' topic")
            //log.debug("LookingFor:$maxItem,$maxValue, $i")
            broker.publish(biddersTopic, LookingFor(maxItem, (0.6 * maxPrice(maxValue))) )
        }
    }

}