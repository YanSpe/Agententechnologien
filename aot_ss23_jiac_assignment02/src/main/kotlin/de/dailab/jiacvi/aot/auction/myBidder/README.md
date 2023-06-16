Problem: ess kommen zu viele Offer an

Scenario:
2023-06-16 18:17:26.737 DEBUG [                            main] d.d.j.s.SerializationProvider    : Loaded class mapping
class [B (Kotlin reflection is not available) : bytes
class java.lang.String (Kotlin reflection is not available) : string
class java.lang.Integer (Kotlin reflection is not available) : integer
class java.lang.Long (Kotlin reflection is not available) : integer
class kotlin.Unit (Kotlin reflection is not available) : integer
interface com.google.protobuf.Message (Kotlin reflection is not available) : protobuf

2023-06-16 18:17:26.738 DEBUG [                            main] d.d.j.d.DefaultDispatcher        : Starting dispatcher with: java.util.concurrent.ThreadPoolExecutor@305a0c5f[Running, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0]
2023-06-16 18:17:26.747 DEBUG [                               /] d.d.j.a.RootGuardian             : Root guardian / initialized
2023-06-16 18:17:26.757  INFO [                            main] d.d.j.AgentSystem                : Enabling module localBroker
2023-06-16 18:17:26.765 DEBUG [                         /system] d.d.j.a.RootGuardian             : Root guardian /system initialized
2023-06-16 18:17:27.783  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Sending Start message to 'all-bidders' topic
2023-06-16 18:17:27.800  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Received StartAuction(maxTurns=2, maxTurnsWithoutOffer=5, turnSeconds=1), Sending Register(bidderId=dummy-1)
2023-06-16 18:17:27.800  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Received StartAuction(maxTurns=2, maxTurnsWithoutOffer=5, turnSeconds=1), Sending Register(bidderId=dummy-2)
2023-06-16 18:17:27.807  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Registered Bidder Register(bidderId=dummy-1).bidderId, Secret 1746264133
2023-06-16 18:17:27.807  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Registered Bidder Register(bidderId=dummy-2).bidderId, Secret -1255613487
2023-06-16 18:17:27.808  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Registered: true
2023-06-16 18:17:27.808  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Registered: true
2023-06-16 18:17:28.783  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Initialized Wallet: Wallet(bidderId=dummy-1, items={Item(type=1)=4, Item(type=2)=3}, credits=500.0), secret: 1746264133
2023-06-16 18:17:28.783  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Initialized Wallet: Wallet(bidderId=dummy-2, items={Item(type=1)=4, Item(type=2)=3}, credits=500.0), secret: -1255613487
2023-06-16 18:17:28.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=2.0)
2023-06-16 18:17:28.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=2.0)
2023-06-16 18:17:28.790  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:28.790  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=1.0)
2023-06-16 18:17:29.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Bidding in Turn 1, (4 open offers)
2023-06-16 18:17:29.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=1): [2.0, 2.0]
2023-06-16 18:17:29.784  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=2.0): OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:29.785  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=2.0): OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:29.785  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:29.785  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:29.785  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=2): [1.0, 1.0]
2023-06-16 18:17:29.785  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:29.786  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:29.786  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=1.0): OfferResult(secret=-1255613487, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:29.786  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Sending Digest to 'all-bidders' topic
2023-06-16 18:17:29.786  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:29.787 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL ITEMS: 14
2023-06-16 18:17:29.787 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL MONEY: 1000.0
2023-06-16 18:17:29.788  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.0)
2023-06-16 18:17:29.788  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=3.0)
2023-06-16 18:17:29.788  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=1.0)
2023-06-16 18:17:29.788  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:29.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.0)
2023-06-16 18:17:29.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:29.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=3.0)
2023-06-16 18:17:29.789  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=2.0)
2023-06-16 18:17:29.791 DEBUG [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=1, itemStats={Item(type=1)=Stats(num=2, min=2.0, median=2.0, max=2.0), Item(type=2)=Stats(num=2, min=1.0, median=1.0, max=1.0)})
2023-06-16 18:17:29.791 DEBUG [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=1, itemStats={Item(type=1)=Stats(num=2, min=2.0, median=2.0, max=2.0), Item(type=2)=Stats(num=2, min=1.0, median=1.0, max=1.0)})
2023-06-16 18:17:30.773  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Bidding in Turn 2, (8 open offers)
2023-06-16 18:17:30.774  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=1): [1.0, 1.0, 3.0, 3.0]
2023-06-16 18:17:30.775  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.0): OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:30.776  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.0): OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:30.777  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=3.0): OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:30.776  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:30.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=1), bid=3.0): OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:30.778  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:30.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=2): [1.0, 1.0, 1.0, 2.0]
2023-06-16 18:17:30.778  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=1), transfer=BOUGHT, price=2.0)
2023-06-16 18:17:30.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=1.0): OfferResult(secret=-1255613487, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:30.778  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=2.0)
2023-06-16 18:17:30.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:30.778  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:30.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:30.779  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:30.779  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:30.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-2, secret=-1255613487, item=Item(type=2), bid=2.0): OfferResult(secret=-1255613487, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:30.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Sending Digest to 'all-bidders' topic
2023-06-16 18:17:30.779  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=-1255613487, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:30.780 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL ITEMS: 14
2023-06-16 18:17:30.780 DEBUG [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=2, itemStats={Item(type=1)=Stats(num=4, min=1.0, median=2.0, max=3.0), Item(type=2)=Stats(num=4, min=1.0, median=1.0, max=2.0)})
2023-06-16 18:17:30.780 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL MONEY: 1000.0
2023-06-16 18:17:30.780 DEBUG [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=2, itemStats={Item(type=1)=Stats(num=4, min=1.0, median=2.0, max=3.0), Item(type=2)=Stats(num=4, min=1.0, median=1.0, max=2.0)})
2023-06-16 18:17:30.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:30.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:30.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:30.781  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:30.781  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:30.781  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:30.781  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.772  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Bidding in Turn 3, (7 open offers)
2023-06-16 18:17:31.773  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=1): [1.99, 1.99, 1.99, 1.99]
2023-06-16 18:17:31.774  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99): OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=1.99)
2023-06-16 18:17:31.775  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99): OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=1.99)
2023-06-16 18:17:31.775  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=1.99)
2023-06-16 18:17:31.776  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99): OfferResult(secret=1746264133, item=Item(type=1), transfer=BOUGHT, price=1.99)
2023-06-16 18:17:31.776  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99): OfferResult(secret=1746264133, item=Item(type=1), transfer=BOUGHT, price=1.99)
2023-06-16 18:17:31.776  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=SOLD, price=1.99)
2023-06-16 18:17:31.777  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Offers for Item(type=2): [1.0, 1.0, 1.0]
2023-06-16 18:17:31.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:31.778  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=BOUGHT, price=1.99)
2023-06-16 18:17:31.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=NONE, price=1.0)
2023-06-16 18:17:31.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Result for Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0): OfferResult(secret=1746264133, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:31.778  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=1), transfer=BOUGHT, price=1.99)
2023-06-16 18:17:31.778  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Sending Digest to 'all-bidders' topic
2023-06-16 18:17:31.778 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL ITEMS: 14
2023-06-16 18:17:31.778  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=SOLD, price=1.0)
2023-06-16 18:17:31.779 DEBUG [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : TOTAL MONEY: 1000.0
2023-06-16 18:17:31.779 DEBUG [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=3, itemStats={Item(type=1)=Stats(num=4, min=1.99, median=1.99, max=1.99), Item(type=2)=Stats(num=3, min=1.0, median=1.0, max=1.0)})
2023-06-16 18:17:31.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.779  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=NONE, price=1.0)
2023-06-16 18:17:31.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:31.779  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result for my Offer: OfferResult(secret=1746264133, item=Item(type=2), transfer=BOUGHT, price=1.0)
2023-06-16 18:17:31.779  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.780 DEBUG [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Received Digest: Digest(turn=3, itemStats={Item(type=1)=Stats(num=4, min=1.99, median=1.99, max=1.99), Item(type=2)=Stats(num=3, min=1.0, median=1.0, max=1.0)})
2023-06-16 18:17:31.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:31.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:31.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:31.780  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=2), bid=1.0)
2023-06-16 18:17:31.781  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Received valid Offer: Offer(bidderId=dummy-1, secret=1746264133, item=Item(type=1), bid=1.99)
2023-06-16 18:17:32.772  INFO [            /auctioneer@6b09f43a] d.d.j.a.a.AuctioneerAgent        : Closing Auction...
2023-06-16 18:17:32.782  INFO [               /dummy-2@4ad00cce] d.d.j.a.a.m.DummyBidderAgent01   : Result of Auction: AuctionResult(score=533, rank=1, finalItems={Item(type=1)=7, Item(type=2)=4}, finalCredits=493.0)
2023-06-16 18:17:32.782  INFO [               /dummy-1@1f038ecc] d.d.j.a.a.m.DummyBidderAgent01   : Result of Auction: AuctionResult(score=510, rank=2, finalItems={Item(type=2)=2, Item(type=1)=1}, finalCredits=507.0)
2023-06-16 18:17:32.783 DEBUG [                        Thread-0] d.d.j.AgentSystem                : Termination initiated
2023-06-16 18:17:37.804  WARN [                        Thread-0] d.d.j.AgentSystem                : Shutdown timeout reached

Process finished with exit code 0

--> Hier zu sehen: es kommen zu viele Offer an. In Runde 2 waren es doppelt so viele Offer, wie in Runde 1
--> In den Wallets  kommt kein neues Item hinzu -> check
--> Offers werden aus Queue gelöscht -> check
--> wie???

Ich denke, dass dadurch auch negative Ergebnisse enstehen, also dass manche Agents in einer Runde 3 oder mehr Items abgezigen bekommen und so eine negativ-Spirale bekommen