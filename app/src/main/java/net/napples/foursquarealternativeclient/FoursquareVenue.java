package net.napples.foursquarealternativeclient;

/**
 * Created by Peppe on 27/11/2015.
 */
public class FoursquareVenue {

    private String name;
    private String city;

    private String address;
    private String checkin;
    private String category;

    public FoursquareVenue() {
        this.name = "";
        this.city = "";
        this.setCategory("");
    }

    public String getCity() {
        if (city.length() > 0) {
            return city;
        }
        return city;
    }

    public void setCity(String city) {
        if (city != null) {
            this.city = city.replaceAll("\\(", "").replaceAll("\\)", "");
            ;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

}
