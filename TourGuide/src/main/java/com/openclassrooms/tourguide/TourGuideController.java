package com.openclassrooms.tourguide;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam String userName){
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    @RequestMapping("/getNearbyAttractions")
    public ResponseEntity<String> getNearbyAttractions(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        JSONArray jsonArray = tourGuideService.getNearByAttractions(visitedLocation);

        // Return the JSONArray as a string and set the content type to application/json
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonArray.toString());
    }
    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }

    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName,
                                       @RequestParam Optional<Integer> numberOfAdultsOpt,
                                       @RequestParam Optional<Integer> numberOfChildrenOpt,
                                       @RequestParam Optional<Integer> tripDurationOpt){
    	return tourGuideService.getTripDeals(getUser(userName),
                numberOfAdultsOpt,
                numberOfChildrenOpt,
                tripDurationOpt);
    }

    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }


}