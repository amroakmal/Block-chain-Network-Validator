// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    TransactionPool txpool = new TransactionPool();

    /*
    * Some helper data structures for easiness of finding any information.
    * The key is the block hash, as of course this will be unique.
    * */
    Map<ByteArrayWrapper, Integer> blockHash_height = new HashMap<>();
    Map<ByteArrayWrapper, UTXOPool> blockHash_utxopool = new HashMap<>();
    Map<ByteArrayWrapper, Block> blockHash_block = new HashMap<>();
    Map<ByteArrayWrapper, Integer> occurence = new HashMap<>();

    // Variable that holds the current max block
    Block maxHeightBlock;

    //The blockchain itself
    ArrayList<Block> chain;

    UTXOPool utxoPool;

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        this.chain = new ArrayList<>();
        this.chain.add(genesisBlock);

        this.utxoPool = new UTXOPool();
        this.maxHeightBlock = genesisBlock;

        UTXO utxoDummy = new UTXO(genesisBlock.getHash(), 0);

        this.utxoPool.addUTXO(utxoDummy, genesisBlock.getCoinbase().getOutput(0));
        this.txpool.addTransaction(genesisBlock.getCoinbase());

        blockHash_height.put(new ByteArrayWrapper(genesisBlock.getHash()), 1);
        blockHash_utxopool.put(new ByteArrayWrapper(genesisBlock.getHash()), this.utxoPool);
        blockHash_block.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisBlock);
        occurence.put(new ByteArrayWrapper(genesisBlock.getHash()), 0);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return this.maxHeightBlock;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return blockHash_utxopool.get(new ByteArrayWrapper(getMaxHeightBlock().getHash()));
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txpool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */

    public boolean addBlock(Block block) {
        if (block == null || block.getPrevBlockHash() == null) {
            return false;
        }

        Block parent = blockHash_block.get(new ByteArrayWrapper(block.getPrevBlockHash()));

        if(blockHash_height.get(new ByteArrayWrapper(parent.getHash())) > CUT_OFF_AGE + 1) {
            blockHash_height.remove(new ByteArrayWrapper(parent.getHash()));
            blockHash_utxopool.remove(new ByteArrayWrapper(parent.getHash()));
            blockHash_block.remove(new ByteArrayWrapper(parent.getHash()));

            UTXO utxo = new UTXO(block.getHash(), occurence.get(new ByteArrayWrapper(block.getHash())));
            this.utxoPool.removeUTXO(utxo);

        } else if(!blockHash_block.containsKey(new ByteArrayWrapper(block.getPrevBlockHash()))) {
            return false;
        }

        TxHandler txHandler = new TxHandler(blockHash_utxopool.get(new ByteArrayWrapper(parent.getHash())));

        // UTXOPool for current new block that will be added
        UTXOPool utxoPool = txHandler.getUTXOPool();

        int currBlockHeight = blockHash_height.get(new ByteArrayWrapper(block.getPrevBlockHash())) + 1;
        int maxForkHeight = blockHash_height.get(new ByteArrayWrapper(getMaxHeightBlock().getHash()));

        if (currBlockHeight > CUT_OFF_AGE + 1) {
            return false;
        }

        if (currBlockHeight > maxForkHeight + 1) {
            return false;
        }

        ArrayList<Transaction> currBlockTxs = block.getTransactions();
        for(Transaction tx : currBlockTxs) {
            if(!txHandler.isValidTx(tx)) {
                return false;
            }
        }

        ArrayList<Transaction> txs = block.getTransactions();
        for(Transaction tx : txs) {
            txpool.addTransaction(tx);
        }

        ArrayList<Transaction.Output> coinBaseOutput = block.getCoinbase().getOutputs();

        int ii = 0;

        for(Transaction.Output txo : coinBaseOutput) {
            UTXO utxo = new UTXO(block.getCoinbase().getHash(), ii++);
            utxoPool.addUTXO(utxo, txo);
        }

        blockHash_utxopool.put(new ByteArrayWrapper(block.getHash()), utxoPool);
        occurence.put(new ByteArrayWrapper(block.getHash()), occurence.size() + 1);
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txpool.addTransaction(tx);
    }
}
