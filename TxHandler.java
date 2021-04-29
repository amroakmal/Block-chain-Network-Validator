import java.util.*;

public class TxHandler {

    private final UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool() {
        return this.utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        /* For eliminating double spending, check if more than one input for thec current
        tx points to same tx source */

        Set<UTXO> utxoVisited = new HashSet<>();
        /* Current tx inputs */
        ArrayList<Transaction.Input> inp = tx.getInputs();
        double sum = 0;
        int idx = 0;
        for (Transaction.Input input : inp) {
            UTXO currInputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
            if(utxoVisited.contains(currInputUTXO)) {
                return false;
            }
            utxoVisited.add(currInputUTXO);
            if(utxoPool.contains(currInputUTXO)) {
                Transaction.Output txo = utxoPool.getTxOutput(currInputUTXO);
                sum += txo.value;
                if(!Crypto.verifySignature(txo.address, tx.getRawDataToSign(idx++), input.signature)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        ArrayList<Transaction.Output> out = tx.getOutputs();
        boolean isValidValue = true;
        for(Transaction.Output output : out) {
            sum -= output.value;
            isValidValue &= (output.value >= 0);
        }
        return sum >= 0 && isValidValue;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> valid = new ArrayList<>();
        int[] visited = new int[possibleTxs.length];
        // Mark transactions that has been processed as visited so as not to process them again
        Arrays.fill(visited, -1);
        while (true) {
            // Check if any transactions are remaining not checked and also valid to be as a root
            boolean anyTransactionFound = false;
            for (int i = 0; i < possibleTxs.length; i++) {
                ArrayList<Transaction.Input> inp = possibleTxs[i].getInputs();
                ArrayList<Transaction.Output> out = possibleTxs[i].getOutputs();
                if(visited[i] == 1) {
                    continue;
                }
                boolean isCandidate = true;
                for (Transaction.Input input : inp) {
                    UTXO currInputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
                    if (!utxoPool.contains(currInputUTXO)) {
                        isCandidate = false;
                        break;
                    }
                }
                //This transaction can be regarded as a root, so let's validate it
                if (isCandidate) {
                    anyTransactionFound = true;
                    visited[i] = 1;
                    if(isValidTx(possibleTxs[i])) {
                        valid.add(possibleTxs[i]);
                        for (Transaction.Input input : inp) {
                            UTXO currInputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
                            utxoPool.removeUTXO(currInputUTXO);
                        }
                        int ii = 0;
                        for (Transaction.Output output : out) {
                            UTXO currInputUTXO = new UTXO(possibleTxs[i].getHash(), ii);
                            utxoPool.addUTXO(currInputUTXO, output);
                            ii++;
                        }
                    }
                }
            }
            // We've made a loop through the whole txs and no one is a root, so just end
            if(!anyTransactionFound) {
                break;
            }
        }
        Transaction[] best = new Transaction[valid.size()];
        for(int i = 0; i < valid.size(); i++) {
            best[i] = valid.get(i);
        }
        return best;
    }
}
