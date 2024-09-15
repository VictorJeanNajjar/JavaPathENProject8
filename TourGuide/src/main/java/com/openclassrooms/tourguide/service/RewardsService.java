package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import gpsUtil.GpsUtil;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private final int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private final int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);


	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void calculateRewards(User user) {
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<VisitedLocation> visitedLocationList = user.getVisitedLocations().stream().collect(Collectors.toList());
		for (VisitedLocation visitedLocation : visitedLocationList) {
			for (Attraction attraction : attractions) {
				if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					calculateDistanceReward(user, visitedLocation, attraction);
				}
			}
		}
	}

	public void calculateDistanceReward(User user, VisitedLocation visitedLocation, Attraction attraction) {
		double distance = getDistance(attraction, visitedLocation.location);
		if (distance <= proximityBuffer) {
			UserReward userReward = new UserReward(visitedLocation, attraction, (int) distance);
			RewardPointsAdding(userReward, attraction, user);
		}
	}

	public void RewardPointsAdding(UserReward userReward, Attraction attraction, User user) {
		CompletableFuture.supplyAsync(() -> {
			synchronized (user) {
				boolean isDuplicate = user.getUserRewards().stream()
						.anyMatch(r -> r.getAttraction().equals(attraction));
				if (isDuplicate) {
					return null;
				}
				int points = rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
				userReward.setRewardPoints(points);
				user.addUserReward(userReward);
				return userReward;
			}
		}, executorService);
	}
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return !(getDistance(attraction, location) > attractionProximityRange);
	}
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
	}

	int getRewardPoints(Attraction attraction, UUID userId) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, userId);
	}

	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}
}