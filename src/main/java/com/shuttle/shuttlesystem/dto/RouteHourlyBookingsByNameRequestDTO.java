package com.shuttle.shuttlesystem.dto;

public class RouteHourlyBookingsByNameRequestDTO {
    private String routeName;
    private String fromDate;
    private String toDate;

    public String getRouteName() {
        return routeName;
    }
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }
    public String getFromDate() {
        return fromDate;
    }
    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }
    public String getToDate() {
        return toDate;
    }
    public void setToDate(String toDate) {
        this.toDate = toDate;
    }
}
