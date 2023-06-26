package de.dailab.jiacvi.aot.auction

import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem
import de.dailab.jiacvi.aot.auction.myBidder.DummyBidderAgent
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent01
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent02
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent03
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent04
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent05
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent06
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent07
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent08
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent09


fun main() {
    agentSystem("Auction") {
        enable(LocalBroker)
        // TODO change the amount of bidders here or the composition if you have different types of agents for testing.
        agents {
            add(AuctioneerAgent(25,
                5, 1))
            for (i in (1..1)) {
                add(DummyBidderAgent("dummy-$i"))
                add(BidderAgent01("BidderAgent01"))
                add(BidderAgent02("BidderAgent02"))
                add(BidderAgent03("BidderAgent03"))
                add(BidderAgent04("BidderAgent04"))
                add(BidderAgent05("BidderAgent05"))
                add(BidderAgent06("BidderAgent06"))
                add(BidderAgent07("BidderAgent07"))
                add(BidderAgent08("BidderAgent08"))
                add(BidderAgent09("BidderAgent09"))
            }
        }
    }.start()
}
