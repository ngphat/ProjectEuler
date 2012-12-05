package ie.cathalcoffey.android.projecteuler;

import java.util.Vector;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class ProblemList extends SherlockActivity implements SearchView.OnQueryTextListener, ActionBar.OnNavigationListener
{
	private SimplerCursorAdapter cursorAdapter;
    private MenuItem loginlogout;
    private String queryText;
    private Spinner spinner;
    private ArrayAdapter<String> spinnerArrayAdapter;
    private SearchView searchView;
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) 
	{
        //Create the search view
        searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint("Search...");
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(false);
        
        menu.add("Search")
            .setIcon(R.drawable.abs__ic_search)
            .setActionView(searchView)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        
        spinner = new Spinner(getSupportActionBar().getThemedContext());
        spinnerArrayAdapter = new ArrayAdapter<String>
        (
            getSupportActionBar().getThemedContext(), 
            R.layout.sherlock_spinner_item, 
            new String[]{"All", "Solved", "Unsolved"}
        );
        spinnerArrayAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
          
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(spinnerArrayAdapter, this);
     
        Intent settings = new Intent(this, Settings.class);
        settings.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        menu.add("Settings")
            .setIntent(settings)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
        if (MyApplication.settings.contains("username"))
        	loginlogout = menu.add("Logout");
        
        else
        	loginlogout = menu.add("Login");
        
        Intent intent = new Intent(this, LoginLogout.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        loginlogout.setIntent(intent);
        loginlogout.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
        
        return true;
    }
	
	@Override
	public void onBackPressed() 
	{
	    this.finish();
	    overridePendingTransition(0, 0);
	}
	
	Receiver receiver;
	
    @Override
	public void onResume() 
	{
	    super.onResume();
	    
	    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("UPDATE_COMPLETE"));
	    
	    if(loginlogout != null)
	    {
		    if (MyApplication.settings.contains("username"))
	        	loginlogout.setTitle("Logout");
	        else
	        	loginlogout.setTitle("Login");
	    }
	    
	    TextView solved = (TextView)findViewById(R.id.solved);
	    
	    int[] counts = {0, 0};
	    if(MyApplication.myDbHelper != null)
	    	counts = MyApplication.myDbHelper.getSolvedCount();
	    
	    solved.setText(String.format("Solved %d of %d", counts[0], counts[1]));
	    
    	cursorAdapter.getFilter().filter("");
	}
	
	@Override
	public void onPause()
	{
	    super.onPause();
	    
	    LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
	
	private class Receiver extends BroadcastReceiver 
	{
		 @Override
		 public void onReceive(Context arg0, Intent arg1) 
		 {
             onResume();
		 }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
	    receiver = new Receiver();
	    
	    if(MyApplication.settings == null)
	        MyApplication.settings = getSharedPreferences("euler", MODE_PRIVATE);
	    
	    if(MyApplication.prefEditor == null)
	        MyApplication.prefEditor = MyApplication.settings.edit();
	    
	    if(MyApplication.myDbHelper == null)
	    {
	        MyApplication.myDbHelper = new MyDataBaseHelper(this);
	        MyApplication.myDbHelper.openDataBase(SQLiteDatabase.OPEN_READWRITE);
	    }
	    
	    getSupportActionBar().setDisplayShowTitleEnabled(false); 
	    
	    setContentView(R.layout.problemlist);
	    	    
	    ListView list = (ListView)findViewById(R.id.list);
	    
	    list.setTextFilterEnabled(true);
	    list.setFastScrollEnabled(true);
	    
        try 
        {
        	String[] from = new String[]{"_id", "title", "solvedby"};
    	    int[] to = new int[]{R.id.id, R.id.title, R.id.solved_by};

    	    Cursor cursor = MyApplication.myDbHelper.getData();
    	    
    	    cursorAdapter = new SimplerCursorAdapter(this, R.layout.row, cursor, from, to);
    		cursorAdapter.setFilterQueryProvider
    		(
    				new FilterQueryProvider()
    				{
						@Override
						public Cursor runQuery(CharSequence constraint) 
						{
							return MyApplication.myDbHelper.getData(MyApplication.filter_text);
						}
    				}
    		);
    		list.setAdapter(cursorAdapter);
    		
    		list.setOnItemClickListener
    		(
    				new OnItemClickListener() 
    				{
						public void onItemClick(AdapterView<?> arg0, View v, int position, long  _id) 
						{
							Intent intent = new Intent(getApplicationContext(), EulerActivity.class);
    		    			
    		    			Bundle bundle = new Bundle();
    		    			bundle.putLong("_id", position);
    		    		    bundle.putString("displayText", MyApplication.display_text);
    		    			bundle.putString("constraint", queryText);
    		    			intent.putExtras(bundle);
    		    			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    		    		    
    		    			Cursor cursor = MyApplication.myDbHelper.getData(queryText);
    		    			
		    				MyApplication.fragments = new Vector<Fragment>();
		    				while (cursor.moveToNext()) 
		    				{
		    				    long _id1 = cursor.getLong(0);
		    				    String title = cursor.getString(1);
		    				    long published = cursor.getLong(2);
		    				    long updated = cursor.getLong(3);
		    				    long solvedby = cursor.getLong(4);
		    				    boolean solved = cursor.getLong(5) == 1 ? true: false;
		    				    String html = cursor.getString(6);
		    				    String answer = cursor.getString(7);
		    				    
		    				    MyApplication.fragments.add(PageFragment.newInstance(_id1, title, published, updated, solvedby, solved, html, answer));
		    				}
		    				
    		    			cursor.close();
		    				
    		    			startActivity(intent);	
						}
                    }
            );
    	
    		int[] counts = MyApplication.myDbHelper.getSolvedCount();
    	    TextView solved = (TextView)findViewById(R.id.solved);
    	    solved.setText(String.format("Solved %d of %d", counts[0], counts[1]));
 	    } 
        
        catch (Exception e) 
        {
 		    throw new Error("Unable to create database");
 	    }
        
        if(!ExampleService.isRunning(this) && MyApplication.settings != null && MyApplication.settings.getBoolean("autoUpdate", true) && MyApplication.settings.contains("username"))
        {
	        Intent serviceIntent = new Intent(ExampleService.ACTION_FOREGROUND);
			serviceIntent.setClass(this, ExampleService.class);
	        startService(serviceIntent);
        }
	}

	@Override
	public void onDestroy()
	{
	    super.onDestroy();
	}
	
	@Override
	public boolean onQueryTextSubmit(String query) 
	{
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) 
	{
		queryText = newText;
		
		MyApplication.filter_text = newText;
    	cursorAdapter.getFilter().filter("");

		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) 
	{
		if(spinnerArrayAdapter != null && spinnerArrayAdapter.getCount() >= position)
		{
		    MyApplication.display_text = spinnerArrayAdapter.getItem(position).toString();
		
		    Cursor c = MyApplication.myDbHelper.getData(MyApplication.filter_text);
		    cursorAdapter.changeCursor(c);
		}
		
		return false;
	}
}
