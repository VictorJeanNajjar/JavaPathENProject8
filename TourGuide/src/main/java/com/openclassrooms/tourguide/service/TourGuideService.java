package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserPreferences;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;


@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	public TripPricer tripPricer = new TripPricer();
	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	public void getUserLocationAsync(User user, TourGuideService tourGuideService) {
		CompletableFuture.supplyAsync(() -> {
					return gpsUtil.getUserLocation(user.getUserId());
				}, executorService)
				.thenAccept(visitedLocation -> { tourGuideService.finalizeLocation(user, visitedLocation); });
	}
	public JSONArray getNearByAttractions(VisitedLocation visitedLocation) {
		// List to store attractions with distance from the user
		List<JSONObject> nearbyAttractions = new ArrayList<>();

		// User's location
		Location userLocation = visitedLocation.location;

		// Loop through all attractions and calculate distances
		for (Attraction attraction : gpsUtil.getAttractions()) {
			// Calculate the distance between user location and attraction
			double distance = rewardsService.getDistance(userLocation, new Location(attraction.latitude, attraction.longitude));

			// Fetch reward points from RewardsCentral
			int rewardPoints = rewardsService.getRewardPoints(attraction, visitedLocation.userId);

			// Create JSON object for each attraction
			JSONObject attractionInfo = new JSONObject();
			attractionInfo.put("attractionName", attraction.attractionName);
			attractionInfo.put("attractionLatitude", attraction.latitude);
			attractionInfo.put("attractionLongitude", attraction.longitude);
			attractionInfo.put("userLatitude", userLocation.latitude);
			attractionInfo.put("userLongitude", userLocation.longitude);
			attractionInfo.put("distance", distance);
			attractionInfo.put("rewardPoints", rewardPoints);

			// Add the object to the list
			nearbyAttractions.add(attractionInfo);
		}

		// Sort the attractions by distance (ascending)
		nearbyAttractions.sort(Comparator.comparingDouble(a -> a.getDouble("distance")));

		// Get the closest 5 attractions
		JSONArray closestAttractions = new JSONArray();
		for (int i = 0; i < Math.min(5, nearbyAttractions.size()); i++) {
			closestAttractions.put(nearbyAttractions.get(i));
		}

		// Return the closest 5 attractions as JSON
		return closestAttractions;
	}
	public List<Provider> getTripDeals(User user, Optional<Integer> numberOfAdultsOpt,
									   Optional<Integer> numberOfChildrenOpt,
									   Optional<Integer> tripDurationOpt){
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		UserPreferences preferences = user.getUserPreferences();
		int numberOfAdults = numberOfAdultsOpt.orElseGet(preferences::getNumberOfAdults);
		int numberOfChildren = numberOfChildrenOpt.orElseGet(preferences::getNumberOfChildren);
		int tripDuration = tripDurationOpt.orElseGet(preferences::getTripDuration);

		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				numberOfAdults, numberOfChildren, tripDuration, cumulativeRewardPoints);

		user.setTripDeals(providers);
		return providers;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Shutdown UserService");
				tracker.stopTracking();
			}
		});
	}


	public VisitedLocation getUserLocation(User user) {
		return user.getVisitedLocations().get(0);
	}

	public void trackUserLocation(User user) {
		getUserLocationAsync(user, this);
	}
	public void finalizeLocation(User user, VisitedLocation visitedLocation) {
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		tracker.finalizeTrack(user);
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public void addUser(User user) {
		internalUserMap.put(user.getUserName(), user);
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}
	public Map<UUID, VisitedLocation> getAllUsersLocations() {
		Map<UUID, VisitedLocation> userLocations = new HashMap<>();

		// Assuming you have a method to get all users
		List<User> users = getAllUsers(); // Method to retrieve all users

		for (User user : users) {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			userLocations.put(user.getUserId(), visitedLocation);
		}

		return userLocations;
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new ConcurrentHashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
