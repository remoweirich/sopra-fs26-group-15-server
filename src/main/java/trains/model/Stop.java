package ch.uzh.ifi.hase.soprafs26.trains.model;


public record Stop(String stopId, String stopName, double lat, double lon, String platform, String sloid) {}