package com.team.editor.server.document;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates readable document share codes.
 */
@Service
public class ShareCodeService {
    public String generateEditorCode() {
        return "EDT-" + randomCodeBody();
    }

    public String generateViewerCode() {
        return "VWR-" + randomCodeBody();
    }

    private String randomCodeBody() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase()
                .substring(0, 6);
    }
}
