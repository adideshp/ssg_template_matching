# Snapshot for template matching
# This example shows off how to use the NCC feature of your OpenMV Cam to match
# image patches to parts of an image... expect for extremely controlled enviorments
# NCC is not all to useful.
#
# WARNING: NCC supports needs to be reworked! As of right now this feature needs
# a lot of work to be made into somethin useful. This script will reamin to show
# that the functionality exists, but, in its current state is inadequate.

import sensor, image
import time, utime
from pyb import UART
from image import SEARCH_EX, SEARCH_DS
from pyb import Pin, LED


led = LED(3)
pin7 = Pin('P7', Pin.IN, Pin.PULL_UP)

# Reset sensor
sensor.reset()

# Set sensor settings
sensor.set_contrast(1)
sensor.set_gainceiling(16)
# Max resolution for template matching with SEARCH_EX is QQVGA
sensor.set_framesize(sensor.QQVGA)
# You can set windowing to reduce the search image.
#sensor.set_windowing(((640-80)//2, (480-60)//2, 80, 60))
#sensor.set_windowing((60,60))
sensor.set_pixformat(sensor.GRAYSCALE)
sensor.skip_frames(time = 2000)

template_path_bmp = "/template.bmp"
template_path_pgm = "/template.pgm"
temp_template_path = "/temp_template.bmp"
stats = {"TOTAL": 0, "MATCHED": 0}


uart = UART(3, 9600, timeout_char=10)
uart.init(9600, bits=8, parity=None, stop=1, timeout_char=10)


def view_template():
    print("VIEW_TEMPLATE")
    with open(template_path_bmp, mode='rb') as file:
        fileContent = file.read()
        uart.write(fileContent);


def take_snap():
    print("TAKE_SNAP")
    temp_img = sensor.snapshot()
    temp_img.save(temp_template_path)
    with open(temp_template_path, mode='rb') as file:
        fileContent = file.read()
        uart.write(fileContent);
        print(".")


def save_snap_as_template():
    print("SAVE_SNAP")
    myImage = image.Image(temp_template_path, copy_to_fb = True)
    myImage.save(template_path_bmp)
    myImage.save(template_path_pgm)


def get_stats():
    uart.write("TOTAL: " + str(stats["TOTAL"]) + "\n");
    uart.write("MATCHED: " + str(stats["MATCHED"]) + "\n");


def stop():
    print("QUIT")
    quit()

functions = {0: stop, 1: view_template, 2: take_snap, 3: save_snap_as_template, 4: get_stats}

"""
while(True):
    sensor.snapshot()"""


#if (int(pin7.value()) == 0):
if (False):
    while(True):
        cmd = uart.readline()
        if(cmd != None):
            print("CMD: " + str(int(cmd)))
            functions[int(cmd)]()
        """if(int(pin7.value()) == 1):
            break"""
else:
    k = 0
    template = image.Image(template_path_pgm, copy_to_fb = False)
    while(True):
        img = sensor.snapshot()
        r = img.find_template(template, 0.70, step=4, search=SEARCH_EX)
        if r:
            led.on()
            time.sleep(100)
            led.off()







