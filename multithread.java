package com.local.version;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;

/**
 * TODO: Document me!
 *
 * @author rajon
 *
 */
public class MultithredInput extends RecordLifecycle {
    private boolean running = false;
    private Thread thread;

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String response = makePostCall();
                    try (FileWriter fw = new FileWriter("F:\\Tracer\\PostResponse.txt.", true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            PrintWriter out = new PrintWriter(bw)) {
                        out.println(response);
                    } catch (IOException e) {
                        // exception handling left as an exercise for the reader
                    }

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (MultithredInput.this) {
                    running = false;
                    MultithredInput.this.notify();
                }
            }
        });
        thread.start();

        TValidationResponse result = null;
        try {
            result = validateResponse(currentRecord);
        } catch (InterruptedException e) {

        }
        return result;
    }

    private TValidationResponse validateResponse(TStructure currentRecord) throws InterruptedException {

        synchronized (this) {
            while (running) {
                wait();
            }
            FundsTransferRecord fundsTransferRecord = new FundsTransferRecord(currentRecord);
            if (!fundsTransferRecord.getDebitCurrency().getValue()
                    .equals(fundsTransferRecord.getCreditCurrency().getValue())) {
                fundsTransferRecord.getDebitCurrency().setError("Debit and Credit Curreny is not Same");
            } else if (Double.parseDouble(fundsTransferRecord.getDebitAmount().getValue()) < 300.00) {
                fundsTransferRecord.getDebitAmount().setOverride("Amount is less than 300");
            }

            return fundsTransferRecord.getValidationResponse();
        }

    }

    public String makePostCall() {
        String cusMne = "JBL123";
        String cusNames = "JANATA";
        String cusName = String.format("\"%s\"", cusNames);
        String cusMnemonic = String.format("\"%s\"", cusMne);

        String POST_PARAMS = "{\n" + "    \"body\":        {\n"
                + "            \"transactionType\": \"AC\",\n"
                + "            \"debitAccount\": \"117722\",\n"
                + "            \"creditAccount\": \"117773\",\n"
                + "            \"debitCurrency\": \"USD\",\n"
                + "            \"debitAmount\": \"1945\",\n"
                + "            \"debitReference\": " + cusMnemonic + ",\n" 
                + "            \"creditReference\": " + cusName + "\n"
                + "        }\n" + "}";

        final String POST_URL = "http://localhost:9089/container-JBLResource/api/v1.0.0/party/payments/funds/transfer/phaseone/beneficiaryConsent";
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(POST_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Basic SU5QVVRUOjEyMzQ1Ng==");
            con.setDoOutput(true); 
            OutputStream os = con.getOutputStream();
            byte[] input = POST_PARAMS.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

}
