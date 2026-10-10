package com.team.editor.client.websocket;

import com.team.editor.common.protocol.dto.*;

public interface OutboundMessageDispatcher {

    void sendJoinRequest(JoinSessionRequest request);

    void sendOperation(OperationMessage message);

    void sendCursorUpdate(CursorUpdateMessage message);

    void sendUndoRequest(UndoRequestMessage message);

    void sendRedoRequest(RedoRequestMessage message);
}
