package net.clintarmstrong.cdpinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File yourFile = new File("/data/data/net.clintarmstrong.cdpinfo/files/tcpdump");
        if(!yourFile.exists()) {
        	new CopyTCPDump().execute();
        }
    }
    
    private class CopyTCPDump extends AsyncTask<Void, Void, Void> {
    	ProgressDialog dialog = new ProgressDialog(MainActivity.this);
    	
    	@Override
        protected void onPreExecute() {
            dialog.setMessage("Copying TCPDump Binary");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }
        
        @Override
        protected void onPostExecute(Void result) {
        	System.gc();
        	dialog.dismiss();
        }

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
        	try {
        		InputStream inputStream = getResources().openRawResource(R.raw.tcpdump);
    			FileOutputStream outputStream = openFileOutput("tcpdump", 0);
    			byte[] buffer = new byte[1024];
    			int length = 0;
                try {
    				while ((length = inputStream.read(buffer)) > 0) {
    				    outputStream.write(buffer, 0, length);
    				}
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	TextView textView=(TextView)findViewById(R.id.output_view);
        	execCommandLine("chmod 777 /data/data/net.clintarmstrong.cdpinfo/files/tcpdump", textView);
			return null;
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    /** Called when the user clicks the Capture button */
    public void captureCDP(View view) {
    	new captureCDP().execute();
    }
    
    private class captureCDP extends AsyncTask<Void, Void, Void> {
    	ProgressDialog dialog = new ProgressDialog(MainActivity.this);
    	
    	@Override
        protected void onPreExecute() {
            dialog.setMessage("Waiting for CDP frame...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					cancel(true);
				}
            });
            dialog.show();
        }
        
        @Override
        protected void onPostExecute(Void result) {
        	dialog.dismiss();
        }

		@Override
		protected Void doInBackground(Void... params) {
        	TextView textView=(TextView)findViewById(R.id.output_view);
        	execCommandLine("/data/data/net.clintarmstrong.cdpinfo/files/tcpdump -i eth0 -nn -v -s 1500 -c 1 'ether[20:2] == 0x2000'", textView);
        	//execCommandLine("/data/data/net.clintarmstrong.cdpinfo/files/tcpdump -nn -v -s 1500 -c 20", textView);
        	return null;
		}
    }
    
 // Root Access script runner
    void execCommandLine(final String command, final TextView tv)
	    {
	    Runtime runtime = Runtime.getRuntime();
	    Process proc = null;
	    OutputStreamWriter osw = null;
	    // Running the Script
	    try
	    {
	    proc = runtime.exec("su");
	    osw = new OutputStreamWriter(proc.getOutputStream());
	    osw.write(command);
	    osw.flush();
	    osw.close();
	    }
	    // If return error
	    catch (IOException ex)
	    {
	    // Log error
	    Log.e("execCommandLine()", "Command resulted in an IO Exception: " + command);
	    return;
	    }
	    // Try to close the process
	    finally
	    {
	    if (osw != null)
	    {
	    try
	    {
	    osw.close();
	    }
	    catch (IOException e){}
	    }
	    }
	    try 
	    {
	    proc.waitFor();
	    }
	    catch (InterruptedException e){}
	    // Display on screen if error
	    if (proc.exitValue() != 0)
	    {
	    Log.e("execCommandLine()", "Command returned error: " + command + "\n Exit code: " + proc.exitValue());
	/**    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    runOnUiThread(new Runnable() {
	        public void run() {
	    	    builder.setMessage(command + "\nWas not executed successfully!");
	    	    builder.setNeutralButton("OK", null);
	    	    AlertDialog dialog = builder.create();
	    	    dialog.setTitle("Script Error");
	    	    dialog.show();  
	       }
	    }); */
	    }
	    BufferedReader reader = new BufferedReader(
	    new InputStreamReader(proc.getInputStream())); 
	    int read;
	    char[] buffer = new char[4096];
	    StringBuffer output = new StringBuffer();
	    try {
	    while ((read = reader.read(buffer)) > 0) {
	    output.append(buffer, 0, read);
	    }
	    } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    }
	    String exit = output.toString();
	    if(exit != null && exit.length() == 0) {
	    exit = "Command executed Successfully but no output was generated";
	    } 
	    final String exited = exit;
	    runOnUiThread(new Runnable() {
	        public void run() {
	        	tv.setText(exited); 
	       }
	   });
	    }
}
