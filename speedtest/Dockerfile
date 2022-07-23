FROM python:3.7.13-buster
RUN apt-get update && sudo apt-get install speedtest-cli
COPY endpoint.py .

EXPOSE 8000

CMD [ "python", "./endpoint.py" ]
