import java.util.*;


public class MaxFeeTxHandler extends TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        super(utxoPool);
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] possibleTxsSortedByFee = Arrays.stream(possibleTxs)
                .map( tx -> new TransactionWrapper(tx))
                .sorted()
                .toArray(Transaction[]::new);
        return super.handleTxs(possibleTxsSortedByFee);
    }

    public class TransactionWrapper implements Comparable<TransactionWrapper> {
        private Transaction tx;
        private Integer fee;

        public TransactionWrapper(Transaction tx) {
            this.tx = tx;

            int sumInputs = 0;
            for(int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input input = tx.getInput(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                sumInputs += utxoPool.getTxOutput(utxo).value;
            }

            int sumOutputs = 0;
            for(int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output output = tx.getOutput(i);
                sumOutputs += output.value;
            }
            this.fee = sumInputs - sumOutputs;
        }

        public int getFee() {
            return fee;
        }

        @Override
        public int compareTo(TransactionWrapper o) {
            return Integer.compare(this.getFee(), o.getFee());
        }
    }
}
