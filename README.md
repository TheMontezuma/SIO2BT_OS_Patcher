# SIO2BT OS Patcher

With this tool you can patch the original ATARI 8-bit OS ROMs
to make them compatible with SIO communication over Bluetooth 

SIO2BT_OS_Patcher detects most common versions of the ATARI OS and let you patch them according to your needs.
Depending on the OS, only some of the parameters will be adjustable (the rest is grayed out in such case):

Timeout
-------
The SIO timeout is the only "MUST HAVE" change if you want to use your patched OS with SIO2BT.
Default OS value (16ms for NTSC / 20ms for PAL) is too small for communication over Bluetooth.
The average answer time over Bluetooth is ~40ms, so you should be fine with 100ms, 
but I recommend to select the largest timeout (240ms for NTSC / 300ms for PAL) to be on the safe side.

Retry count
-----------
Retry count can be decreased to minimize waiting time, when ATARI starts without a disk drive.
You can compensate bigger timeouts with a smaller number of re-tries.
Default OS value is 13 and the recommended value for SIO2BT is 5.

Disable poll for new devices
----------------------------
ATARI XL/XE tries (at start-up) to detect "new devices" with so called "3F" poll.
This procedure is explained in the XL Addendum to the Operating System Manual.
However no such device exists and the "3F" poll delays startup when ATARI is powered on without a disk drive.
I recommend to disable the poll.

Enable Coldstart with Shift+Reset
---------------------------------
It is a convenience feature to force a coldstart when the shift+reset button combination is pressed.
When SIO2BT is powered from the ATARI, you can use it to load new games without a need to re-establish Bluetooth connection every time.

Select Atarimax Bank 0 at Coldstart
-----------------------------------
Optionally the shift+reset button combination may also reset Atarimax multicart cartridges.
Writing at $D500 activates bank 0, where a cart menu is located.
As a result pressing shift+reset would not only trigger the ATARI coldstart, but also reset the Atarimax cart.
This option is available only together with the previous one and it has no relation to SIO2BT.

Default Basic Off
-----------------
You may invert the logic and make BASIC disabled per default.
In that case you have to keep OPTION key pressed at start-up to activate BASIC.

Enable HiSpeed
--------------
You can patch XL and XE OS ROMs with the HighSpeed patch from HIAS (https://github.com/HiassofT/highspeed-sio).
The patchrom tool has now -b parameter to support SIO2BT for all SIO devices (SIO2BT emulates also SmartDevice and NetworkingDevice), including baudrate detection.
And with the SIO2BT OS Patcher you can apply the same patch.