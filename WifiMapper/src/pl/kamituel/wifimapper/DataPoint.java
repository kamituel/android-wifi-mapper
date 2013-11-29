package pl.kamituel.wifimapper;

import java.util.LinkedList;
import java.util.List;

import pl.kamituel.wifimapper.views.Dot;

public class DataPoint<T> implements Dot {
	private List<T> mMeasurements = new LinkedList<T>();
	
	@Override
	public String getLabel() {
		return "" + mMeasurements.size();
	}
	
	public void addMeasurement(T measurement) {
		mMeasurements.add(measurement);
	}
	
	public List<T> getMeasurements() {
		return mMeasurements;
	}
}
