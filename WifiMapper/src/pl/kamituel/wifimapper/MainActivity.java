package pl.kamituel.wifimapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.kamituel.wifimapper.WifiScanner.OnWifiScan;
import pl.kamituel.wifimapper.views.Dot;
import pl.kamituel.wifimapper.views.DotSquareView;
import android.app.ActionBar;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ActionBar.OnNavigationListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    private DummySectionFragment mCurrentFragment;
   
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[] {
                                getString(R.string.title_section1),
                                getString(R.string.title_section2),
                                getString(R.string.title_section3),
                        }),
                this);
    }
    
    public void saveResult(View v) {
    	mCurrentFragment.saveResult(v);
    }
 
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        mCurrentFragment = new DummySectionFragment();
        Bundle args = new Bundle();
        args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
        mCurrentFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mCurrentFragment)
                .commit();
        return true;
    }

    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public static class DummySectionFragment extends Fragment implements OnWifiScan {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";
        
        private int mDimension;
        private final static String TAG = DummySectionFragment.class.getCanonicalName();
        private DotSquareView mDotView;
        private List<DataPoint<List<ScanResult>>> mDataPoints;
        private WifiScanner mWifiScanner;
        
        public DummySectionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
            final int MIN_DIMENSION = 3;
            
            
            mDotView = (DotSquareView) rootView.findViewById(R.id.dotView);
            SeekBar dimensionSeekBar = (SeekBar) rootView.findViewById(R.id.dimensionSeekBar);
                 
            dimensionSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					TextView dimensionTextView = (TextView) rootView.findViewById(R.id.dimensionTextView);
					// "+ MIN_DIMENSION" because SeekBar has minimum value = 0 and it's not configurable.
					mDimension = progress + MIN_DIMENSION;
					dimensionTextView.setText("" + mDimension);
					init();
				}
			});
            
			mDimension = dimensionSeekBar.getProgress() + MIN_DIMENSION;
            
            return rootView;
        }
        
        @Override
		public void onPause() {
			super.onPause();
			mWifiScanner.stop();
		}

		@Override
		public void onResume() {
			super.onResume();
			init();
		}

		private void init() {
			populateDataPoints(mDimension);
        	mDotView.setDots(mDimension, (List<Dot>)(List<?>) mDataPoints);
        	
        	if (mWifiScanner == null) { 
        		mWifiScanner = new WifiScanner(getActivity(), this);
				mWifiScanner.start();
        	}
		}
		
        private void populateDataPoints(int dimension) {
        	mDataPoints = new ArrayList<DataPoint<List<ScanResult>>>(dimension * dimension);
        	for (int d = 0; d < dimension * dimension; d += 1) {
        		mDataPoints.add(new DataPoint<List<ScanResult>>());
        	}
        }

		@Override
		public void onWifiScan(List<ScanResult> result) {
			DataPoint<List<ScanResult>> selected = (DataPoint<List<ScanResult>>) mDotView.getSelectedDot();
			if (selected == null) {
				Log.d(TAG, "Skipping data point, selected none.");
				return;
			}
			
			Log.d(TAG, "point: " + result.size());
			
			selected.addMeasurement(result);
			mDotView.postInvalidate();
		}
     
		public void saveResult(View v) {			
			try {
				JSONObject result = new JSONObject();

				for (int d = 0; d < mDataPoints.size(); d += 1) {
					DataPoint<List<ScanResult>> point = mDataPoints.get(d);
					int row = d / mDimension;
					int column = d % mDimension;
					
					JSONArray measurements = new JSONArray();
					Iterator<List<ScanResult>> measurementsIt = point.getMeasurements().iterator();
					while (measurementsIt.hasNext()) {
						Iterator<ScanResult> mIt = measurementsIt.next().iterator();
						
						JSONArray networks = new JSONArray();
						while (mIt.hasNext()) {
							ScanResult scan = mIt.next();
							JSONObject scanJson = new JSONObject();
							scanJson.put("bssid", scan.BSSID);
							scanJson.put("frequency", scan.frequency);
							scanJson.put("level", scan.level);
							scanJson.put("timestamp", scan.timestamp);
							scanJson.put("ssid", scan.SSID);
							networks.put(scanJson);
						}
						measurements.put(networks);
					}
					
					result.put(String.format("%d,%d", row, column), measurements);
				}
				
				Log.d(TAG, "RESULT");
				Log.d(TAG, result.toString());
				
				Calendar c = Calendar.getInstance();
				String filename = String.format("%4d-%02d-%02d_%02d_%02d_%02d.json", 
						c.get(Calendar.YEAR), 
						c.get(Calendar.MONTH) + 1, 
						c.get(Calendar.DAY_OF_MONTH),
						c.get(Calendar.HOUR_OF_DAY),
						c.get(Calendar.MINUTE),
						c.get(Calendar.SECOND));
				boolean saved = saveFile(filename, result.toString());
				
				if (saved) {
					Toast.makeText(getActivity(), "saved as " + filename, Toast.LENGTH_LONG).show();
					init();
				}
			} catch (JSONException e) {
				Toast.makeText(getActivity(), "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show();
				Log.e(TAG, "", e);
			}
		}
		
		private boolean saveFile(String filename, String contents) {
			File out = new File(Environment.getExternalStorageDirectory(), filename);
			FileWriter writer = null;
			try {
				writer = new FileWriter(out, false);
				writer.write(contents);
				writer.flush();
			} catch (IOException e) {
				Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
				Log.e(TAG, "" ,e);
				return false;
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			return true;
		}
    }
}
