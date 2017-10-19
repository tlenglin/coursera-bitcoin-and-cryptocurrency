public class TxHandler {

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
        UTXOPool seenUTXOs = new UTXOPool();
        double ConsumedCoinSum = 0;
        double valueProducedSum = 0;
        int i = 0;

        for (i < tx.numInputs(); i++){
            Transaction.Input in = tx.getInput(i);

            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);

            if (!utxoPool.contains(utxo)){
                //1st condition verified
                return false;
            }

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), in.signature)){
                //2nd condition verified
                return false;
            }
            if (seenUTXOs.contains(utxo)){
                //3rd condition verified
                return false;
            }

            seenUTXOs.addUTXO(utxo, output);
            ConsumedCoinSum += output.value;
        }

        for (Transaction.Output out : tx.getOutputs()){
            if (out.value < 0){
                // 4th conditon verified
                return false;
            }
            valueProducedSum += out.value;
        }
        
        if (valueProducedSum < ConsumedCoinSum){
            // 5th condition verified
            return false;
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTransactionsArrayList = new ArrayList<>();

        boolean done = false;

        while(!done){
            done = true;

            for(int i = 0; i < possibleTxs.length; i++){
                if(possibleTxs[i] == null){
                    continue;
                }

                if(isValidTx(possibleTxs[i])){
                    ArrayList<Transaction.Input> inputs = possibleTxs[i].getInputs();
                    for(int j = 0; j < inputs.size(); j++){
                        Transaction.Input tempInput = inputs.get(j);
                        UTXO tempUTXO = new UTXO(tempInput.prevTxHash, tempInput.outputIndex);
                        //remove this UTXO because it has now been consumed
                        utxoPool.removeUTXO(tempUTXO);
                    }

                    ArrayList<Transaction.Output> outputs = possibleTxs[i].getOutputs();
                    for(int j = 0; j < outputs.size(); j++){
                        Transaction.Output tempOutput = outputs.get(j);
                        UTXO tempUTXO = new UTXO(possibleTxs[i].getHash(), j);
                        utxoPool.addUTXO(tempUTXO, tempOutput);
                    }

                    validTransactionsArrayList.add(possibleTxs[i]);

                    possibleTxs[i++] = null;

                    done = false;
                }
            }
        }

        Transaction[] validTransactions = new Transaction[validTransactionsArrayList.size()];
        validTransactions = validTransactionsArrayList.toArray(validTransactions);
        return validTransactions;
        
    }

}
