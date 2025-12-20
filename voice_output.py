import sys
import pyttsx3

engine = pyttsx3.init()

# text from command line
text = " ".join(sys.argv[1:])

if text.strip():
    engine.say(text)
    engine.runAndWait()