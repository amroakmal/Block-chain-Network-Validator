pragma solidity ^0.5.1;

contract MyContract {
    
    uint256 deposite;
    address payable manager;
    uint256 startBidTime;
    uint256 endBidTime;
    uint256 revealTimeStart;
    uint256 revealTimeEnd;
    uint256 finalizeStartTime;
    uint256 finalizeEndTime;
    
    //struct for storing needed value for a bidder
    struct Bidder {
        address payable _address;
        uint256 _value;
    }

    //variable to store the address of the winner user
    address payable public maxBidAddress;
    
    //variable to store the maximum bid value has been paid
    uint256 public maxBidValue = 0;
    
    //stroing for each address the corresponding commit value generated/produced
    mapping(address => bytes32) public hashes;
    
    //storing addresses for users who are able to get their money back
    mapping(address => uint256) public toWithdraw;
    
    Bidder[] public honest;
    
    constructor (uint256 deposite_value, uint256 bidTime, uint256 revealTime, uint256 finalizeTime) public {
        deposite = deposite_value;
        manager = msg.sender;
        startBidTime = now;
        endBidTime = startBidTime + bidTime;
        revealTimeStart = endBidTime;
        revealTimeEnd = revealTimeStart + revealTime;
        finalizeStartTime = revealTimeEnd;
        finalizeEndTime = finalizeStartTime + finalizeTime;
    }
    
    /*
    
    Act as local functions which will generate for the user his commitment, by hashing:
    user address, user nonce, and uswr value to be used for bidding.
    
    bytes32 public tempHash;
    
    function test(string memory nonce, uint256 _value) public {
        tempHash = keccak256(abi.encodePacked(msg.sender, _value, nonce));
    }
    
    */
    
    //modifier checking if the current person is not the manager
    modifier aBidder() {
        require(msg.sender != manager);
        _;
    }
    
    //modifier checking if the current person is the manager
    modifier theManager() {
        require(msg.sender == manager);
        _;
    }
    
    //modifier checking the current value is greater than the deposite value
    modifier acceptableBidValue() {
        require(msg.value > deposite);
        _;
    }
    
    //modifier checking the current interval running is for the bidding stage
    modifier biddingTimeRunning() {
        require(now >= startBidTime && now <= endBidTime);
        _;
    }
    
    //modifier checking the current interval running is for the revealing stage
    modifier revealTimeRunning() {
        require(now >= revealTimeStart && now <= revealTimeEnd);
        _;
    }
    
    //modifier checking the current interval running is for the finalize stage
    modifier finalizeTimeStarted() {
        require(now >= finalizeStartTime && now <= finalizeEndTime);
        _;
    }
    
    //modifier checking the current user is eligible to get back his money
    modifier eligibleForWithdrawal() {
        require(toWithdraw[msg.sender] != 0);
        _;
    }
    
    //Called by each user with a constraint to call it within its time range
    function MakeBid(bytes32 commit) public payable aBidder biddingTimeRunning  {
        require(msg.value > 0);
        hashes[msg.sender] = commit;
    }
    
    //Called by each user as many times as he wants with a constraint to call it within its time range
    function Reveal(string memory nonce) public payable revealTimeRunning  {
        
        bytes32 precomputed;
        bytes32 claimedHash;
        
        //Get the stored hash value for this user
        precomputed = hashes[msg.sender];
        
        //Get the current hash for this user with his input such as nonce and claimed value he paid for Bid
        claimedHash = keccak256(abi.encodePacked(msg.sender, msg.value, nonce));
        
        
        //If both hashes are the same, this user is honest
        if(precomputed == claimedHash) {
            honest.push(Bidder(msg.sender, msg.value));
        }
    }
    
    //Called by the manager, to get the maximum bid value which has been paid
    function Finalize() public payable theManager finalizeTimeStarted {
        uint256 maxBidIdx = 0;
        uint256 secondMax = 0;
        
        /*Choose the maximum bid value, and get the index in the array, and the address of the user
        who paid the maximum value*/
        for(uint256 i = 0; i < honest.length; i++) {
            if(maxBidValue < honest[i]._value) {
                maxBidValue = honest[i]._value;
                maxBidAddress = honest[i]._address;
                maxBidIdx = i;
            }
        }
        
        /*
        For each user who is honest but not winner, gather them with each other for withdarwal operation
        to be made by them
        */
        
        for(uint256 i = 0; i < honest.length; i++) {
            if(i != maxBidIdx) {
                toWithdraw[honest[i]._address] = honest[i]._value;
                if(honest[i]._value > secondMax) {
                    secondMax = honest[i]._value;
                }
            }
        }
        //Winner gets the difference between his bid value and second maximum value, so he only
        //pays the second maximum bid value
        maxBidAddress.transfer(maxBidValue - secondMax);
    }
    
    //Check if the executer of the function is really an honest user and not winner, if so he can now get his money
    function GetMoneyForHonestAndLoser() public payable eligibleForWithdrawal {
        msg.sender.transfer(toWithdraw[msg.sender] + deposite);
    }
}
