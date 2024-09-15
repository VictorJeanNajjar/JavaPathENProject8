package com.openclassrooms.tourguide;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import gpsUtil.location.Location;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

public class TestRewardsService {

    @Test
    public void usersGetRewards() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(100000);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        tourGuideService.addUser(user);
        assertTrue(user.getUserRewards().isEmpty());

        Attraction attraction = gpsUtil.getAttractions().get(0);
        user.clearVisitedLocations();
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
        rewardsService.calculateRewards(user);

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
        }

        tourGuideService.tracker.stopTracking();
        List<UserReward> userRewards = user.getUserRewards();

        assertFalse(userRewards.isEmpty());
    }

	@Test
	public void isWithinAttractionProximity() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		Attraction attraction = gpsUtil.getAttractions().get(0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}
	@Test
	public void nearAllAttractions() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);
        List<Attraction> attractions = gpsUtil.getAttractions();
        User user = new User(UUID.randomUUID(),"asd","asfd","asd");

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        for (Attraction attraction : attractions){
            UserReward userReward = new UserReward(new VisitedLocation(UUID.randomUUID(), new Location(0,0),new Date()),attraction,1);
            user.addUserReward(userReward);
        }
		rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
        List<UserReward> userRewards = tourGuideService.getUserRewards(user);


		assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}

}