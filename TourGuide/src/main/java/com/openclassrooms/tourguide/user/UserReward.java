package com.openclassrooms.tourguide.user;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import java.util.Objects;

public class UserReward {

	public final VisitedLocation visitedLocation;
	public final Attraction attraction;
	private int rewardPoints;
	public UserReward(VisitedLocation visitedLocation, Attraction attraction, int rewardPoints) {
		this.visitedLocation = visitedLocation;
		this.attraction = attraction;
		this.rewardPoints = rewardPoints;
	}
	
	public UserReward(VisitedLocation visitedLocation, Attraction attraction) {
		this.visitedLocation = visitedLocation;
		this.attraction = attraction;
	}

	public void setRewardPoints(int rewardPoints) {
		this.rewardPoints = rewardPoints;
	}
	
	public int getRewardPoints() {
		return rewardPoints;
	}

	public VisitedLocation getVisitedLocation() {
		return visitedLocation;
	}

	public Attraction getAttraction() {
		return attraction;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserReward that = (UserReward) o;
		return rewardPoints == that.rewardPoints &&
				Objects.equals(visitedLocation, that.visitedLocation) &&
				Objects.equals(attraction, that.attraction);
	}

	@Override
	public int hashCode() {
		return Objects.hash(visitedLocation, attraction, rewardPoints);
	}
}
