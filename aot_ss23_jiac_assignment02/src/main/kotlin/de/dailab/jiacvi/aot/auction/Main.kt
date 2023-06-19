package de.dailab.jiacvi.aot.auction

import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent01
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent02
import de.dailab.jiacvi.aot.auction.myBidder.BidderAgent03
import de.dailab.jiacvi.aot.auction.myBidder.DummyBidderAgent


fun main() {
    agentSystem("Auction") {
        enable(LocalBroker)
        // TODO change the amount of bidders here or the composition if you have different types of agents for testing.
        agents {
            add(AuctioneerAgent(5,
                5, 1))
            for (i in (1..9)) {
                add(BidderAgent03("dummy-$i"))
            }
        }
    }.start()
}
