# Improved Zigbee Hue Bulb
An Improved Zigbee Hue Bulb device type for SmartThings

I ran into a lot of issues trying to get my scenes to line up with Hue bulbs paired directly to the SmartThings Hub.  A lot of this was due to the original SmartThings device not "roundtripping" information. 

This improved driver increases the the robustness of these return values.  Also, it will correctly mark the bulb as "off" if it becomes unreachable. 
