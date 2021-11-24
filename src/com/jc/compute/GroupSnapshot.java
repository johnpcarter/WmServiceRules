package com.jc.compute;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class GroupSnapshot {
		
	public Date time;
	
	public List<Value> values;
	
	public GroupSnapshot(Date time) {
		this.values = new ArrayList<Value>();
		this.time = time;
	}
	
	public void add(String label, Double value) {
		
		((ArrayList<Value>) this.values).add(new Value(label, value));
	}
	
	public void add(String label, Double value, int violationLevel) {
		
		((ArrayList<Value>) this.values).add(new Value(label, value, violationLevel));
	}
	
	public static class Value {
		
		public Double value;
		public String label;
		public int level = 0;
		public boolean didFireARule = false;
		
		Value(String label, Double value) {
			this.label = label;
			this.value = value;
		}
		
		Value(String label, Double value, int violationLevel) {
			this.label = label;
			this.value = value;
			this.didFireARule = true;
			this.level = violationLevel;
		}
	}

	public IData toIData(Map<String, String> colors, double scale) {
		
		IData out = IDataFactory.create();
		IDataCursor c = out.getCursor();
		
		IDataUtil.put(c, "time", formatTime(this.time));

		int i = 0;
		IData[] vals = new IData[this.values.size()];
		
		for (Value v : this.values) {
			
			IData vout = IDataFactory.create();
			IDataCursor vc = vout.getCursor();
			IDataUtil.put(vc, "label", v.label);			
			IDataUtil.put(vc, "violationLevel", v.level);
			IDataUtil.put(vc,  "didFire", v.didFireARule);
			IDataUtil.put(vc, "value", (long) Math.rint(v.value));
			
			if (scale > 0 && v.value != 0) {
				IDataUtil.put(vc, "scaledValue", v.value / scale);
			} else {
				IDataUtil.put(vc, "scaledValue", v.value);
			}
			
			IDataUtil.put(vc, "color", colors.get(v.label));
			vc.destroy();

			vals[i++] = vout;
		}
		
		IDataUtil.put(c, "values", vals);
		c.destroy();
		
		return out;
	}
	
	private static String formatTime(Date date) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		
		return sdf.format(date);
	}
}
