package io.lucamoser.io.tempdiff;

import com.google.gson.annotations.SerializedName;

public class TempResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("result")
    private Result[] result;

    public Result[] getResult() {
        return result;
    }

    public class Result {
        @SerializedName("station")
        private String station;
        @SerializedName("values")
        private Values values;

        public String getStation() {
            return station;
        }

        public Values getValues() {
            return values;
        }
    }

    public class Values {
        @SerializedName("air_temperature")
        private AirTemperature airTemperature;

        public AirTemperature getAirTemperature() {
            return airTemperature;
        }
    }

    public class AirTemperature {
        @SerializedName("value")
        private double value;

        public double getValue() {
            return value;
        }
    }

}
