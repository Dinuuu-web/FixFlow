package com.fixflow.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiRequestDto {
    private String action;   // improve, category, priority, troubleshoot, summary
    private String content;  // complaint text / description
    private String category; // optional hint
    private String complaintId; // optional for summary action
}
