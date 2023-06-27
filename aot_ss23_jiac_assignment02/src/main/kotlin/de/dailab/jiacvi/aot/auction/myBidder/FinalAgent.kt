package de.dailab.jiacvi.aot.auction.myBidder

import de.dailab.jiacvi.aot.auction.*
import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import kotlin.math.pow
import kotlin.system.exitProcess

class FinalAgent(private val id: String) : Agent(overrideName = id) {
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
            log.debug("Received Digest: {}", it)
            itemStats.add(it.itemStats)
            log.info(itemStats.size.toString())
            lookingFor()
            bid()
        }

        // save LookingFors
        listen<LookingFor>(biddersTopic) {
            log.debug("Received LookingFor: {}", it)
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
        // calculate median price from previous bidding rounds and LookingFors
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
        // calculate varianz from medians
        var varianz = 0.0
        for (digest in itemStats) {
            val newMedian = digest[item]?.median
            if (newMedian != null) {
                val pow = (newMedian - median).pow(2.0)
                varianz += pow * (1 / itemStats.size)
            }
        }
        return varianz
    }

    private fun getValueOfAdditionalItem(number: Int): Double {
        if (number >= 0) {
            return (fib(number + 1).toDouble())
        } else {
            return ((fib(-number) * 2).toDouble())
        }
    }

    private fun getValueOfItem(number: Int): Double {
        if (number >= 0) {
            return fib(number).toDouble()
        } else {
            return ((fib(-(number - 1)) * 2).toDouble())
        }
    }

    private fun getPrice(item: MutableMap.MutableEntry<Item, Int>, shortSelling: Boolean): Double? {
        val valueOfItem = getValueOfItem(item.value)
        val valueOfAdditionalItem = getValueOfAdditionalItem(item.value)

        if (itemStats.isNotEmpty()) {
            val median = getMedian(item.key)
            val varianz = getVarianz(item.key, median)

            if (shortSelling) {
                if (valueOfAdditionalItem > median) {
                    // BUY - lower median, but still get the item
                    val newPrice = median + varianz
                    return if (newPrice >= valueOfItem) newPrice
                    else null

                } else if (valueOfAdditionalItem == median) {
                    // logic from DummyFib(n)Agent
                    return if (valueOfAdditionalItem >= valueOfItem) valueOfItem
                    else null

                } else {
                    // SELL - increase median, but still sell the item
                    val newPrice = median - varianz
                    return if (newPrice >= valueOfItem) newPrice
                    else null
                }
            } else {
                if (item.value >= (getMaxValue() - (0.2 * itemStats[0].size).toInt())) {
                    // BUY - item with high value
                    log.info("ItemStats:" + itemStats[0].size.toString())
                    // Bid between value and median
                    return getValueOfAdditionalItem(item.value) - ((getValueOfAdditionalItem(item.value) - median) / 2)
                }
                else {
                    // SELL - item with low value
                    // Bid only when profit can be made
                    val newPrice = median - varianz
                    return if (newPrice in valueOfItem..valueOfAdditionalItem) newPrice
                    // logic from DummyFib(n)Agent
                    else return valueOfItem
                }
            }
        // logic from DummyFib(n)Agent
        } else return valueOfItem
    }

    private fun bid() {
        val ref = system.resolve(auctioneer)
        if (wallet != null) {
            for (item in wallet!!.items) {
                var optimalPrice: Double?
                if (item.value <= 0) {
                    optimalPrice = getPrice(item, true)
                } else {
                    optimalPrice = getPrice(item, false)
                }
                if (optimalPrice != null && optimalPrice <= wallet!!.credits) {
                    ref invoke ask<Boolean>(Offer(id, secret, item.key, optimalPrice)) {
                    }
                }
            }
        }
    }

    private fun getMaxValue(): Int {
        // find highest value of Wallet
        if (wallet != null) {
            var maxValue = -1
            for (item in wallet!!.items) {
                if (item.value > maxValue) {
                    maxValue = item.value
                }
            }
            return maxValue
        }
        return 0
    }

    private fun lookingFor() {
        // find Item with the highest value
        if (wallet != null) {
            var maxItem = Item(-1)
            var maxValue = -1
            for (item in wallet!!.items) {
                if (item.value > maxValue) {
                    maxItem = item.key
                    maxValue = item.value
                }
            }
            // send LookingFor
            log.info("Sending LookingFor to '$biddersTopic' topic")
            broker.publish(biddersTopic, LookingFor(maxItem, (0.6 * getMedian(maxItem))))
        }
    }

}