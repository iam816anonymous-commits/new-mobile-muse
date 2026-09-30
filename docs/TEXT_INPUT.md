# LocalAgent Text Input & Screen Capture Subsystems

## Text Input (`TextInputController`)
Defines contract actions for `setText`, `appendText`, `clearText`, and `paste` without keyboard injection hacks.

## Screen Capture (`ScreenCaptureController`)
Defines MediaProjection user-consented screen capture capability contracts, distinguishing `AVAILABLE`, `USER_CONSENT_REQUIRED`, `PERMITTED`, and `UNAVAILABLE`.
