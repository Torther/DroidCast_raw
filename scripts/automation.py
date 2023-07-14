#!/usr/local/bin/python -u

import subprocess
import webbrowser
import argparse
import signal
from threading import Timer

adb_command = ['adb']

parser = argparse.ArgumentParser(
    description='Automation script to activate capturing screenshot of Android device')
parser.add_argument('-s', '--serial', dest='device_serial',
                    help='Device serial number (adb -s option)')
parser.add_argument(
    '-p',
    '--port',
    dest='port',
    nargs='?',
    const=53516,
    type=int,
    default=53516,
    help='Port number to be connected, by default it\'s 53516')
args_in = parser.parse_args()


def run_adb(args, pipe_output=True):
    if args_in.device_serial:
        args = adb_command + ['-s', args_in.device_serial] + args
    else:
        args = adb_command + args

    output = None
    if pipe_output:
        output = subprocess.PIPE

    process = subprocess.Popen([str(arg) for arg in args], stdout=output, encoding='utf-8')
    stdout, stderr = process.communicate()
    return process.returncode, stdout, stderr


def locate_apk_path():
    return_code, output, _ = run_adb(["shell", "pm", "path", "ink.mol.droidcast_raw"])
    if return_code or output == "":
        raise RuntimeError("Locating apk failure, have you installed the app successfully?")

    prefix = "package:"
    postfix = ".apk"
    begin_index = output.index(prefix, 0)
    end_index = output.rfind(postfix)

    return "CLASSPATH=" + output[begin_index + len(prefix):(end_index + len(postfix))].strip()


def open_browser():
    url = 'http://localhost:%d/preview' % args_in.port
    webbrowser.open_new(url)


def identify_device():
    return_code, output, _ = run_adb(["devices"])
    if return_code:
        raise RuntimeError("Fail to find devices")
    else:
        print(output)
        device_serial_no = args_in.device_serial
        devices_info = str(output)
        device_count = devices_info.count('device') - 1

        if device_count < 1:
            raise RuntimeError("Fail to find devices")

        if device_count > 1 and not device_serial_no:
            raise RuntimeError("Please specify the serial number of target device you want to use ('-s serial_number').")


def print_url():
    return_code, output, _ = run_adb(["shell", "ip route | awk '/wlan*/{ print $9 }'| tr -d '\n'"])
    print("\n>>> Share the url 'http://%s:%d/preview' to see the live screen! <<<\n" % (output, args_in.port))


def handler(signum, frame):
    print('\n>>> Signal caught: ', signum)
    return_code, _, _ = run_adb(["forward", "--remove", "tcp:%d" % args_in.port])
    print(">>> adb unforward tcp:%d " % args_in.port, return_code)


def automate():
    signal.signal(signal.SIGINT, handler)

    try:
        identify_device()
        class_path = locate_apk_path()

        return_code, _, _ = run_adb(["forward", "tcp:%d" % args_in.port, "tcp:%d" % args_in.port])
        print(">>> adb forward tcp:%d " % args_in.port, return_code)

        print_url()

        arguments = ["shell", class_path, "app_process", "/", "ink.mol.droidcast_raw.Main", "--port=%d" % args_in.port]

        timer = Timer(2, open_browser)
        timer.start()

        run_adb(arguments, pipe_output=False)

    except Exception as e:
        print(e)


if __name__ == "__main__":
    automate()
