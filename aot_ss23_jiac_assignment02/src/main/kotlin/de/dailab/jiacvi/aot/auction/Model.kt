package de.dailab.jiacvi.aot.auction

/*
 * MESSAGES BETWEEN AUCTIONEER AND BIDDER
 */

enum class Transfer {
    NONE, SOLD, BOUGHT
}

// Auctioneer -> Bidders (publish)
data class StartAuction(val maxTurns: Int, val maxTurnsWithoutOffer: Int, val turnSeconds: Int)

// Bidder -> Auctioneer (ask), replied with Boolean
data class Register(val bidderId: String)

// Auctioneer -> Bidder (tell)
data class Registered(val secret: Int, val numBidders: Int, val items: Map<Item, Int>, val credits: Price)


// Bidder -> Auctioneer (ask), replied with Boolean
data class Offer(val bidderId: String, val secret: Int, val item: Item, val bid: Price)

// Auctioneer -> Bidder (tell)
data class OfferResult(val secret: Int, val item: Item, val transfer: Transfer, val price: Price)

// Bidder -> Auctioneer (ask), replied with CashInResult
data class CashIn(val bidderId: String, val secret: Int, val item: Item, val num: Int)
data class CashInResult(val price: Price)


// Auctioneer -> Bidders (publish)
data class Digest(val turn: Int, val itemStats: Map<Item, Stats>)
data class Stats(val num: Int, val min: Price, val median: Price, val max: Price)

// Bidder -> Bidders (publish)
data class LookingFor(val item: Item, val price: Price)


// Auctioneer -> Bidder (tell)
data class AuctionResult(val score: Int, val rank: Int, val finalItems: Map<Item, Int>, val finalCredits: Price)

/*
 * HELPER CLASSES
 */

typealias Price = Double

// wrapper/placeholder/alias for actual item; might also be replaced with just Int or String
data class Item(val type: Int)

/**
 * helper class for managing a bidder agent's wallet, both for the auctioneer and the actual bidder
 */
data class Wallet(val bidderId: String, val items: MutableMap<Item, Int>, var credits: Price) {

    fun update(item: Item, deltaItem: Int, deltaCredits: Price) {
        items[item] = items.getOrDefault(item, 0) + deltaItem
        if (items[item] == 0) items.remove(item)
        credits += deltaCredits
    }

    fun value(): Int {
        val creditsScore = if (credits >= 0) credits else 2 * credits
        val itemsScore = items.values.sumBy {
            if (it >= 0) fibsum(it) else fibsum(-it) * -2
        }
        return creditsScore.toInt() + itemsScore
    }
}


/*
 * HELPER FUNCTIONS
 */

// not optimized at all, but okay for small n
fun fib(n: Int): Int = if (n < 2) n else fib(n-1) + fib(n-2)

fun fibsum(n: Int) = (1..n).map(::fib).sum()

fun median(list: List<Double>) = list.sorted().let {
    if (it.size % 2 == 0)
        (it[it.size / 2] + it[(it.size - 1) / 2]) / 2
    else
        it[it.size / 2]
}
