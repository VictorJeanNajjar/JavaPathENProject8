package com.openclassrooms.tourguide;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.openclassrooms.tourguide.user.UserPreferences;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import tripPricer.Provider;
import tripPricer.TripPricer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestTourGuideService {

    @Mock
    private final GpsUtil gpsUtil = new GpsUtil();
    private final RewardCentral rewardCentral = new RewardCentral();
    private final RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);
    @InjectMocks
    private final TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
    @Mock
    private TripPricer tripPricer;
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    public void getUserLocation() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        tourGuideService.trackUserLocation(user);
        while(user.getVisitedLocations().isEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        }
        tourGuideService.tracker.stopTracking();

        assertEquals(user.getVisitedLocations().get(0).userId, user.getUserId());
    }
    @Test
    public void addUser() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        User retrievedUser = tourGuideService.getUser(user.getUserName());
        User retrievedUser2 = tourGuideService.getUser(user2.getUserName());

        assertEquals(user, retrievedUser);
        assertEquals(user2, retrievedUser2);
    }

    @Test
    public void getAllUsers() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        List<User> allUsers = tourGuideService.getAllUsers();

        assertTrue(allUsers.contains(user));
        assertTrue(allUsers.contains(user2));
    }

    @Test
    public void trackUser() throws Exception {
        // Create a user with mock data
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        // Simulate the behavior of the GpsUtil.getUserLocation method (if needed in the service)
        Location location = new Location(0, 0);
        VisitedLocation visitedLocation = new VisitedLocation(user.getUserId(), location, new Date());
        user.addToVisitedLocations(visitedLocation);
        // Call the trackUserLocation method, which is void
        tourGuideService.trackUserLocation(user);

        // Wait briefly for the location tracking to complete (or use mock to simulate this immediately)
        // Check if the visited location has been added to the user's visited locations
        List<VisitedLocation> visitedLocations = user.getVisitedLocations(); // Assuming this method exists

        // Check that the last visited location matches the expected values
        assertFalse(visitedLocations.isEmpty()); // Ensure there's at least one location
        VisitedLocation result = visitedLocations.get(visitedLocations.size() - 1); // Get the most recent location

        // Assert that the user ID matches
        assertEquals(user.getUserId(), result.userId);
    }


    @Test
    public void getNearbyAttractions() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Location location = new Location(0, 0);
        VisitedLocation visitedLocation = new VisitedLocation(user.getUserId(), location, new Date());

        // Create mock attractions
        List<Attraction> attractions = gpsUtil.getAttractions();

        // Replace the actual GPS Util's attractions list with our test list
        // Assume gpsUtil.getAttractions() returns our test list
        // Note: You need to modify GpsUtil to allow setting attractions if necessary
        JSONArray nearbyAttractions = tourGuideService.getNearByAttractions(visitedLocation);

        assertEquals(5, nearbyAttractions.length());
    }
    @Test
    public void getTripDeals() throws Exception {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        user.setUserPreferences(new UserPreferences());
        Optional<Integer> numberOfAdultsOpt = Optional.empty();
        Optional<Integer> numberOfChildrenOpt = Optional.empty();
        Optional<Integer> tripDurationOpt = Optional.empty();

        // Mock the response from tripPricer.getPrice
        List<Provider> mockProviders = List.of(
                new Provider(UUID.randomUUID(), "Provider1", 100.0),
                new Provider(UUID.randomUUID(), "Provider2", 200.0),
                new Provider(UUID.randomUUID(), "Provider3", 300.0),
                new Provider(UUID.randomUUID(), "Provider4", 400.0),
                new Provider(UUID.randomUUID(), "Provider5", 500.0)
        );

        when(tripPricer.getPrice(
                anyString(),          // API key
                any(UUID.class),     // Attraction ID
                anyInt(),             // Number of adults
                anyInt(),             // Number of children
                anyInt(),             // Trip duration (nights stay)
                anyInt()              // Reward points
        )).thenReturn(mockProviders);

        // Call the method to get the list of providers
        List<Provider> resultProviders = tourGuideService.getTripDeals(user, numberOfAdultsOpt, numberOfChildrenOpt, tripDurationOpt);

        // Verify the interaction with tripPricer
        verify(tripPricer).getPrice(
                anyString(),          // API key
                eq(user.getUserId()), // User ID
                eq(user.getUserPreferences().getNumberOfAdults()), // Number of adults
                eq(user.getUserPreferences().getNumberOfChildren()), // Number of children
                eq(user.getUserPreferences().getTripDuration()), // Trip duration
                anyInt()              // Reward points (calculated cumulatively)
        );

        // Check that the result contains the expected number of providers
        assertEquals(5, resultProviders.size());
        assertEquals(mockProviders, resultProviders);
        assertEquals(mockProviders, user.getTripDeals()); // Ensure user’s tripDeals is also updated
    }
}

