package com.macro.app.input;

interface IShizukuInputProbe {
    int getProcessUid();
    int getProcessPid();
    boolean hasInjectEventsPermission();
}
