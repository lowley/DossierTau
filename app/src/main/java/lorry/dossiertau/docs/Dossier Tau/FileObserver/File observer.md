## Constants

### ACCESS
public static final int ACCESS
Event type: ==Data was read from a file==
Constant Value: 1 (0x00000001)
### ALL_EVENTS
public static final int ALL_EVENTS
Event mask: ==All valid event types, combined== 
==Value is either `0` or a combination of all==
Constant Value: 4095 (0x00000fff)
### ATTRIB
public static final int ATTRIB
Event type: ==Metadata (permissions, owner, timestamp) was changed explicitly==
Constant Value: 4 (0x00000004)
### CLOSE_NOWRITE
public static final int CLOSE_NOWRITE
Event type: ==Someone had a file or directory open read-only, and closed it==
Constant Value: 16 (0x00000010)

### CLOSE_WRITE
public static final int CLOSE_WRITE
Event type: ==Someone had a file or directory open for writing, and closed it==
Constant Value: 8 (0x00000008)

### CREATE       🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢
public static final int CREATE
Event type: ==A new file or subdirectory was created under the monitored directory==
Constant Value: 256 (0x00000100)

### DELETE       🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢
public static final int DELETE
Event type: ==A file was deleted from the monitored directory==
Constant Value: 512 (0x00000200)

### DELETE_SELF
public static final int DELETE_SELF
Event type: ==The monitored file or directory was deleted; monitoring effectively stops==
Constant Value: 1024 (0x00000400)

### MODIFY
public static final int MODIFY
Event type: ==Data was written to a file==
Constant Value: 2 (0x00000002)

### MOVED_FROM
public static final int MOVED_FROM
Event type: ==A file or subdirectory was moved from the monitored directory==
Constant Value: 64 (0x00000040)

### MOVED_TO
public static final int MOVED_TO
Event type: ==A file or subdirectory was moved to the monitored directory==
Constant Value: 128 (0x00000080)

### MOVE_SELF
public static final int MOVE_SELF
Event type: ==The monitored file or directory was moved; monitoring continues==
Constant Value: 2048 (0x00000800)

### OPEN
public static final int OPEN
Event type: ==A file or directory was opened==
Constant Value: 32 (0x00000020)