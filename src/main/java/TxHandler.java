import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
        List<UTXO> claimedUTXOs = new ArrayList<>();
        int sumInputs = 0;
        for(int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            if(!utxoPool.contains(utxo)) {
                return false;
            }

            // (2) the signatures on each input of {@code tx} are valid
            if(!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address , tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            // (3) no UTXO is claimed multiple times by {@code tx}
            if(!claimedUTXOs.contains(utxo)) {
                return false;
            }
            claimedUTXOs.add(utxo);
            sumInputs += utxoPool.getTxOutput(utxo).value;
        }

        int sumOutputs = 0;
        for(int i = 0; i < tx.numOutputs(); i++) {
            // (4) all of {@code tx}s output values are non-negative
            Transaction.Output output = tx.getOutput(i);
            if(output.value < 0) {
                return false;
            }
            sumOutputs += output.value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        // values; and false otherwise.
        if(sumInputs < sumOutputs) {
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> validTransactions = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                for(int i = 0; i < tx.numInputs(); i++) {
                    Transaction.Input input = tx.getInput(i);
                    utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }
                for(int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    utxoPool.addUTXO(new UTXO(tx.getHash(), i), output);
                }
                validTransactions.add(tx);
            }
        }
        return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }

}
