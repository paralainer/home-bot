FROM balenalib/raspberry-pi-python:3.7-buster
RUN sudo apt-get update && sudo apt-get install speedtest-cli
COPY endpoint.py .

EXPOSE 8000

CMD [ "python", "./endpoint.py" ]
