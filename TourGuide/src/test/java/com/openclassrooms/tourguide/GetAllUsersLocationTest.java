package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class GetAllUsersLocationTest {

    @Mock
    private GpsUtil gpsUtil; // Mocked dependency

    @InjectMocks
    private TourGuideService tourGuideService; // Class under test

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllUsersLocations() {
        // Create test users
        User user1 = new User(UUID.randomUUID(), "user1", "000", "user1@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "user2", "000", "user2@tourGuide.com");

        // Create VisitedLocation for test users
        Location location1 = new Location(40.748817, -73.985428); // Example coordinates
        Location location2 = new Location(34.052235, -118.243683); // Example coordinates

        VisitedLocation visitedLocation1 = new VisitedLocation(user1.getUserId(), location1, new Date());
        VisitedLocation visitedLocation2 = new VisitedLocation(user2.getUserId(), location2, new Date());


        tourGuideService.addUser(user1);
        tourGuideService.addUser(user2);

        // Mock gpsUtil to return the mocked VisitedLocations
        when(gpsUtil.getUserLocation(user1.getUserId())).thenReturn(visitedLocation1);
        when(gpsUtil.getUserLocation(user2.getUserId())).thenReturn(visitedLocation2);
        System.out.println(tourGuideService.getAllUsers());
        // Call the method under test
        Map<UUID, VisitedLocation> result = tourGuideService.getAllUsersLocations();
        System.out.println(result);
        // Verify the results
        assertEquals(2, result.size()-1);
    }
}

