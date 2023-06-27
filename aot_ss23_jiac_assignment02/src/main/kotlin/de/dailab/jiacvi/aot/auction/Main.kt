package de.dailab.jiacvi.aot.auction

import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem
import de.dailab.jiacvi.aot.auction.myBidder.`DummyFib(n+1)Agent`
import de.dailab.jiacvi.aot.auction.myBidder.MaxTypeAgent
import de.dailab.jiacvi.aot.auction.myBidder.MedianAgent01
import de.dailab.jiacvi.aot.auction.myBidder.MedianAgent02
import de.dailab.jiacvi.aot.auction.myBidder.MedianAgent03
import de.dailab.jiacvi.aot.auction.myBidder.BangForBuckAgent01
import de.dailab.jiacvi.aot.auction.myBidder.BangForBuckAgent02
import de.dailab.jiacvi.aot.auction.myBidder.ShortSellingAgent01
import de.dailab.jiacvi.aot.auction.myBidder.ShortSellingAgent02
import de.dailab.jiacvi.aot.auction.myBidder.LookingForAgent01
import de.dailab.jiacvi.aot.auction.myBidder.LookingForAgent02
import de.dailab.jiacvi.aot.auction.myBidder.MaxValueAgent
import de.dailab.jiacvi.aot.auction.myBidder.DummyBetweenAgent
import de.dailab.jiacvi.aot.auction.myBidder.`DummyFib(n)Agent`
import de.dailab.jiacvi.aot.auction.myBidder.FinalAgent

fun main() {
    agentSystem("Auction") {
        enable(LocalBroker)
        // TODO change the amount of bidders here or the composition if you have different types of agents for testing.
        agents {
            add(AuctioneerAgent(10,
                5, 1))
            for (i in (1..1)) {
                add(`DummyFib(n+1)Agent`("fib(n+1)Agent"))
                add(MaxTypeAgent("MaxTypeAgent"))
                add(MedianAgent01("MedianAgent01"))
                add(MedianAgent02("MedianAgent02"))
                add(MedianAgent03("MedianAgent03"))
                add(BangForBuckAgent01("BangForBuckAgent01"))
                add(BangForBuckAgent02("BangForBuckAgent02"))
                add(LookingForAgent01("LookingForAgent01"))
                add(LookingForAgent02("LookingForAgent02"))
                add(MaxValueAgent("LookingForAgent03"))
                add(ShortSellingAgent01("ShortSellingAgent01"))
                add(ShortSellingAgent02("ShortSellingAgent02"))
                add(DummyBetweenAgent("AlwaysInTheMiddleAgent"))
                add(`DummyFib(n)Agent`("fib(n)Agent"))
                add(FinalAgent("FinalAgent"))
            }
        }
    }.start()
}
