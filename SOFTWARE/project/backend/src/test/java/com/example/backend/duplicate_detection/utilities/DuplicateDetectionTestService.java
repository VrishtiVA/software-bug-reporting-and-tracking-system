package com.example.backend.duplicate_detection.utilities;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class DuplicateDetectionTestService {

    private final MockMvc mockMvc;
    public DuplicateDetectionTestService(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @WithUserDetails("va104")
    public void rejectAllDuplicates() throws Exception {
        do {
            MvcResult duplicateClaimed = mockMvc.perform(post("/api/workspaces/1/duplicates/resolve/claim")).andReturn();
            if (duplicateClaimed.getResponse().getStatus() == 200) {
                MvcResult claim = mockMvc.perform(get("/api/workspaces/1/duplicates/resolve")).andReturn();
                Integer claimedFromReport = JsonPath.parse(claim.getResponse().getContentAsString()).read("$.from_report");
                mockMvc.perform(
                    post("/api/workspaces/1/duplicates/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("from_report:"+ claimedFromReport +", to_report: null, is_accepted: false")
                );
            } else break;
        } while (true);
    }

}
