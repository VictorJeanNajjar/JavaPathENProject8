package com.openclassrooms.tourguide;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

import static org.junit.jupiter.api.Assertions.*;

public class TestPerformance {

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remains consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100,000 users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		Attraction attraction = gpsUtil.getAttractions().get(0);
		InternalTestHelper.setInternalUserNumber(10);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		List<User> allUsers = tourGuideService.getAllUsers();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for(User user : allUsers) {
			user.clearVisitedLocations();
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
		}

		for(User user : allUsers) {
			while(user.getUserRewards().isEmpty()) {
				try {
					TimeUnit.MILLISECONDS.sleep(200);
				} catch (InterruptedException ignored) {
				}
			}
		}
		tourGuideService.tracker.stopTracking();

		stopWatch.stop();
		System.out.println("Seconds ellapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));

		for(User user: allUsers) {
            assertFalse(user.getUserRewards().isEmpty());
		}
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	@Test
	public void highVolumeTrackLocation() throws InterruptedException, ExecutionException {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(10);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		List<User> allUsers = tourGuideService.getAllUsers();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for(User user : allUsers) {
			tourGuideService.trackUserLocation(user);
		}

		for(User user : allUsers) {
			while(user.getVisitedLocations().size() < 4) {
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException ignored) {
				}
			}
		}

		for(User user: allUsers) {
			VisitedLocation visitedLocation = user.getVisitedLocations().get(3);
            assertNotNull(visitedLocation);
		}
		tourGuideService.tracker.stopTracking();

		stopWatch.stop();
		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}


}
