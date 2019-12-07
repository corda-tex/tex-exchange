Run the commands as per the sequence below

Setup
./gradlew clean deployNodes
./build/nodes/runnodes/

IMPORTANT:
Ensure Notary, PartyA, PartyB and BankOfEngland nodes are running. If not, cd into ./build/nodes/[Node Name Here] and java -jar corda.jar 

PartyA: Self issue stock tokens and list order
flow start SelfIssueStockTokenFlow ticker: GOOG, quantity: 1000
flow start OrderListFlow2 ticker: GOOG, units: 10, price: 25, expires: "2019-12-28T18:30:00.000Z"

Query PartyA vault for stock tokens
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken

Query PartyA and/or B vault(s) for listed order state
run vaultQuery contractStateType: com.ccc.state.Order2

PartyB: Attempt to buy order - should fail with exception (insufficient funds) - the first time this is executed there is no progress tracker for some reason (bug). Progress tracker is visible from second time around onwards
flow start OrderBuyFlow2 linearId: [Order linearId]

Issue GBP from BankOfEngland and move to PartyB
flow start IssueCashTokenFlow quantity: 1000.00
flow start MoveCashTokenFlow quantity: 250.00, recipient: PartyB

Query PartyB vault for GBP
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken

PartyB: Buy order with funds
flow start OrderBuyFlow2 linearId: [Order Id]

Query PartyB vault for stock tokens and observe GBP tokens have moved
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken

Query PartyA vault for GBP tokens and reduced # of stock tokens
run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken