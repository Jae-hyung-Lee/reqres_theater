insert into MOVIE_TABLE(ID, MOVIE_ID, STATUS, TITLE)
VALUES (1, 'MOVIE-00001', 'OPENED', '어벤져스');
insert into MOVIE_TABLE(ID, MOVIE_ID, STATUS, TITLE)
VALUES (2, 'MOVIE-00002', 'OPENED', '아이언맨');
insert into MOVIE_TABLE(ID, MOVIE_ID, STATUS, TITLE)
VALUES (3, 'MOVIE-00003', 'OPENED', '토르');
insert into MOVIE_TABLE(ID, MOVIE_ID, STATUS, TITLE)
VALUES (4, 'MOVIE-00004', 'OPENED', '분노의 질주');
insert into MOVIE_TABLE(ID, MOVIE_ID, STATUS, TITLE)
VALUES (5, 'MOVIE-00005', 'OPENED', '미션 임파서블');

insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, PAY_ID, SEAT_ID, STATUS)
VALUES (1, 'MOVIE-00001', 'CLIENT-01', 'A1', 'BOOK');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, SEAT_ID, STATUS)
VALUES (2, 'MOVIE-00001', 'B1', '');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, SEAT_ID, STATUS)
VALUES (3, 'MOVIE-00001', 'C1', '');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, PAY_ID, SEAT_ID, STATUS)
VALUES (4, 'MOVIE-00001', 'CLIENT-02', 'D1', 'BOOK');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, PAY_ID, SEAT_ID, STATUS)
VALUES (5, 'MOVIE-00001', 'CLIENT-03', 'E1', 'BOOK');

insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, SEAT_ID, STATUS)
VALUES (6, 'MOVIE-00002', 'A1', 'BOOK');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, PAY_ID, SEAT_ID, STATUS)
VALUES (7, 'MOVIE-00002', 'CLIENT-01', 'B1', '');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, SEAT_ID, STATUS)
VALUES (8, 'MOVIE-00002', 'C1', '');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, SEAT_ID, STATUS)
VALUES (9, 'MOVIE-00002', 'D1', '');
insert into MOVIE_SEAT_TABLE(ID, MOVIE_ID, PAY_ID, SEAT_ID, STATUS)
VALUES (10, 'MOVIE-00002', 'CLIENT-04', 'E1', 'BOOK');
