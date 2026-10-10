package com.team.editor.server.service;

import com.team.editor.common.protocol.message.SessionRole;
import com.team.editor.server.session.ConnectedUser;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    /**
     * Checks whether the connected user is allowed to send edit operations.
     * The role is assigned by SessionJoinService from document access, not trusted from the client.
     */
    public boolean canEdit(ConnectedUser user) {
        return user != null && user.getRole() == SessionRole.EDITOR;
    }
}