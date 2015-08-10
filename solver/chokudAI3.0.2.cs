using System;
using System.Collections.Generic;
using System.Web;
using System.Text;
using System.Globalization;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Diagnostics;




class ICFPC2015
{
    string version = "chokudAIver3.0.2";
    int DIV = 50;
    static bool debug = false;
    bool activeunit = false;
    static int limit = 1 * 60 * 1000;
    static int reallimit;

    int addrand;
    Dictionary<int, int> checkedwidth;

    [DataContract]
    class Data
    {
        public Data()
        {
        }

        [DataMember]
        public int id;
        [DataMember]
        public Unit[] units;
        [DataMember]
        public int width;
        [DataMember]
        public int height;
        [DataMember]
        public Cell[] filled;
        [DataMember]
        public int sourceLength;
        [DataMember]
        public int[] sourceSeeds;
    }

    [DataContract]
    class Unit
    {
        [DataMember]
        public Cell[] members;
        [DataMember]
        public Cell pivot;

        public Cell[,] allmember;
        public int MM;

        public void moveFirstPlace(int H, int W){
            int minY = 999;
            int minX = 999;
            int maxX = 0;
            foreach (var item in members)
            {
                minY = Math.Min(minY, item.y);
                maxX = Math.Max(maxX, item.x);
                minX = Math.Min(minX, item.x);
            }
            foreach (var item in members)
            {
                item.y -= minY;
                if (minY % 2 == 1)
                {
                    if (item.y % 2 == 0) item.x--;
                    else item.x++;
                }
            }
            pivot.y -= minY;

            int movex = 0;
            while ((minX + movex) / 2 > ((W - 1) - (maxX + movex)) / 2) movex -= 2;
            while ((minX + (movex + 2)) / 2 <= (((W - 1) - (maxX + (movex + 2)))) / 2) movex += 2;
            
            foreach (var item in members)
            {
                item.x += movex;
            }
            pivot.x += movex;

            foreach (var item in members)
            {
                item.x -= pivot.x;
                item.y -= pivot.y;
            }
            allmember = new Cell[members.Length, 6];
            List<int>[] l = new List<int>[6];

            for (int i = 0; i < members.Length; i++)
            {
                allmember[i, 0] = members[i];
                for (int j = 1; j < 7; j++)
                {
                    int px = allmember[i, j - 1].x;
                    int py = allmember[i, j - 1].y;
                    if(j < 6) allmember[i, j] = new Cell((px - 3 * py) / 2, (px + py) / 2);
                    //else Console.WriteLine(((px - 3 * py) / 2) + " " + ((px + py) / 2) + " " + allmember[i, 0].x + " " + allmember[i, 0].y);
                }
            }

            for (int j = 0; j < 6; j++)
            {
                l[j] = new List<int>();
                for (int i = 0; i < members.Length; i++)
                {
                    l[j].Add(allmember[i, j].x * 1123139871 ^ allmember[i, j].y * 189628736);
                }
                l[j].Sort();
            }

            MM = 6;
            for (int j = 1; j < 6; j++)
            {
                bool flag = true;
                for (int i = 0; i < members.Length; i++)
                {
                    if (l[0][i] != l[j][i])
                    {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                {
                    MM = j;
                    break;
                }
            }
        }
    }

    [DataContract]
    class Cell
    {
        public Cell(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        [DataMember]
        public int x;
        [DataMember]
        public int y;

        public void AdjustCell(int H, int W){
            x = 2 * x + (y % 2);
        }
    }

    [DataContract]
    class Output
    {
        [DataMember]
        public int problemId;
        [DataMember]
        public int seed;
        [DataMember]
        public string tag;
        [DataMember]
        public string solution;
    }



    class State: IComparable<State>
    {
        public int point;
        public int score;
        public int[,] board;
        public string command;
        public int old_ls;
        public int usedflag;

        public int CompareTo(State s)
        {
            return -point.CompareTo(s.point);
        }

        public long hash;
        public static long[,] hashboard;
        public static int W, H;
        public static void sethash(int _W, int _H, Random r)
        {
            W = _W;
            H = _H;
            hashboard = new long[W, H];
            for (int i = 0; i < W; i++)
            {
                for (int j = 0; j < H; j++)
                {
                    hashboard[i, j] = ((long)r.Next() << 31) + r.Next();
                }
            }
        }

        public long gethash()
        {
            if (hash != 0) return hash;
            hash = 1;
            int l1 = board.GetLength(0);
            int l2 = board.GetLength(1);
            for (int i = 0; i < l1; i++)
            {
                for (int j = 0; j < l2; j++)
                {
                    if (board[i, j] == 1)
                    {
                        hash ^= hashboard[i, j];
                    }
                }
            }
            return hash;
        }


        long nexthash(long a, long b)
        {
            return (a * 1629871623716381L ^ (a >> 22) * 17576531113131 ^ b * 765218735128371 ^ (b >> 17) * 165234916131);
        }
    }

    Random r;

    public ICFPC2015() { }

    static string filename;
    static List<string> phrase;
    Stopwatch sw1;
    Stopwatch sw2;


    public static void Main(string[] args)
    {
        foreach (var item in args)
        {
            Console.Error.WriteLine(item);
        }
        int argsnum = 0;

        phrase = new List<string>();

        while (args.Length > argsnum)
        {
            if (args[argsnum] == "-f")
            {
                argsnum++;
                filename = args[argsnum];
                argsnum++;
            }
            else if (args[argsnum] == "-p")
            {
                argsnum++;
                phrase.Add(args[argsnum].ToLowerInvariant());
                argsnum++;
            }
            else if (args[argsnum] == "-d")
            {
                debug = true;
                argsnum++;
            }
            else if (args[argsnum] == "-t")
            {
                argsnum++;
                limit = (int)(double.Parse(args[argsnum]) * 1000);
                argsnum++;
            }
            else
            {
                argsnum++;
            }
        }

        new ICFPC2015().myon();
    }

    HttpClient hc;

    int W, H, WW;
    int N;
    int M;
    int[] source;
    Data d;

    List<State> nowsl;
    List<State> nextsl;
    Dictionary<long, int> predic;
    Dictionary<long, int> dic;




    public class LCG
    {

        long mod = 1L << 32;
        long mul = 1103515245;
        long inc = 12345;
        long prev;

        public LCG(long seed)
        {
            prev = seed;
        }

        public int next()
        {
            int ret = (int)(prev >> 16) & 0x7fff;
            prev = (mul * prev + inc) % mod;
            return ret;
        }
    }

    string gettingtext()
    {
        StreamReader sr = new StreamReader(
                filename, Encoding.GetEncoding("Shift_JIS"));

        string st = sr.ReadToEnd();
        Console.Error.WriteLine(st);
        return st;
    }

    State beststate;

    int addR = 1;

    bool validcheck(string iast, int MM)
    {
        int y = 0;
        int x = 0;
        int p = 0;
        Dictionary<int, int> tdic = new Dictionary<int, int>();
        tdic[0] = 1;
        foreach (var c in iast)
        {
            int k = charToMove[c];
            if (k < 4)
            {
                y += vy[k];
                x += vx[k];
            }
            else
            {
                p = (p + vp[k - 4] + MM) % MM;
            }
            int next = (y * 1000000) + (x *1000) + p;
            if (tdic.ContainsKey(next)) return false;
            tdic[next] = 1;
        }
        return true;

    }

    void myon()
    {

        sw1 = new Stopwatch();
        sw2 = new Stopwatch();

        sw1.Start();
        reallimit = limit * 9 / 10;

        bool first = true;
        string json = gettingtext();
        var serializer = new DataContractJsonSerializer(typeof(Data));
        var stream1 = new MemoryStream(Encoding.UTF8.GetBytes(json));
        d = (Data)serializer.ReadObject(stream1);
        r = new Random();
        W = d.width;
        H = d.height;
        WW = W + W;

        State.sethash(WW, H, r);

        charToMove = new Dictionary<char, int>();
        charToMove['p'] = 0;
        charToMove['!'] = 0;
        charToMove['\''] = 0;
        charToMove['.'] = 0;
        charToMove['0'] = 0;
        charToMove['3'] = 0;
        charToMove['b'] = 3;
        charToMove['c'] = 3;
        charToMove['e'] = 3;
        charToMove['f'] = 3;
        charToMove['y'] = 3;
        charToMove['2'] = 3;
        charToMove['a'] = 1;
        charToMove['g'] = 1;
        charToMove['h'] = 1;
        charToMove['i'] = 1;
        charToMove['j'] = 1;
        charToMove['4'] = 1;
        charToMove['l'] = 2;
        charToMove['m'] = 2;
        charToMove['n'] = 2;
        charToMove['o'] = 2;
        charToMove[' '] = 2;
        charToMove['5'] = 2;
        charToMove['d'] = 4;
        charToMove['q'] = 4;
        charToMove['r'] = 4;
        charToMove['v'] = 4;
        charToMove['z'] = 4;
        charToMove['1'] = 4;
        charToMove['k'] = 5;
        charToMove['s'] = 5;
        charToMove['t'] = 5;
        charToMove['u'] = 5;
        charToMove['w'] = 5;
        charToMove['x'] = 5;

        for (int i = 0; i < 26; i++)
        {
            char c = (char)('a' + i);
            int temp = charToMove[c];
        }
        for (int i = 0; i < 6; i++)
        {
            char c = (char)('0' + i);
            int temp = charToMove[c];
        }

        //d.sourceLength = 10;

        validdic = new Dictionary<string, bool>[7];
        for (int i = 1; i < 7; i++)
        {
            validdic[i] = new Dictionary<string, bool>();
            foreach (var iast in phrase)
            {
                validdic[i][iast] = validcheck(iast, i);
            }
        }


        foreach (var item in d.units)
        {
            foreach (var i2 in item.members)
            {
                i2.AdjustCell(H, WW);
            }
            item.pivot.AdjustCell(H, WW);
            item.moveFirstPlace(H, WW);
        }

        foreach (var item in d.filled)
        {
            item.AdjustCell(H, WW);
        }

        foreach (var item in d.units)
        {
            if (item.members.Length > 1) activeunit = true;
        }


        for (int i = 0; i < phrase.Count; i++)
        {
            if (phrase[i] == "ia! ia!")
            {
                iaiaid = i;
                break;
            }
        }

        foreach (var seed in d.sourceSeeds)
        {
            N = d.sourceLength;
            M = d.units.Length;
            source = new int[N];
            LCG lcg = new LCG(seed);
            bestscore = 0;
            beststring = "";
            beststep = 0;

            for (int i = 0; i < N; i++)
            {
                source[i] = lcg.next() % M;
            }

            
            State firststate = new State();
            firststate.board = new int[WW, H];
            firststate.command = "";
            firststate.score = 0;
            firststate.old_ls = 0;
            firststate.usedflag = 0;
            
            foreach (var item in d.filled)
            {
                firststate.board[item.x, item.y] = 1;
            }

            checkedwidth = new Dictionary<int, int>();
            sumstep = 0;

            for (int i = 0; i < 10 && sw1.ElapsedMilliseconds < reallimit; i++)
            {
                if (i == 0)
                {
                    firstwidth = (int)((double)100000 / W / H / d.sourceLength * reallimit / (1 * 60 * 1000));
                    if(firstwidth < 1) firstwidth = 1;
                }
                else
                {
                    int use = (int)sw1.ElapsedMilliseconds + 30;
                    int nokori = (reallimit - use) * 4 / 5;
                    if (beststep < d.sourceLength / 2) nokori /= 2;
                    firstwidth = (int)((double)(sumstep + 1) * nokori / use / d.sourceLength);
                    if (firstwidth < 1) firstwidth = 1;
                }
                if (checkedwidth.ContainsKey(firstwidth))
                {
                    addR = H * 2;
                }
                else addR = 1;
                checkedwidth[firstwidth] = 0;

                dic = new Dictionary<long, int>();
                dic[0] = 0;
                nowsl = new List<State>();
                nowsl.Add(firststate);
                beamSearch();
                nowsl.Sort();
                string ret = beststring;
                Output rr = new Output();
                rr.seed = seed;
                rr.problemId = d.id;
                rr.tag = version + '@' + bestscore;
                rr.solution = ret;

                Console.Error.WriteLine(d.id + " " + seed + ": " + bestscore);

                MemoryStream ms = new MemoryStream();
                var serializer2 = new DataContractJsonSerializer(typeof(Output));
                serializer2.WriteObject(ms, rr);
                string JsonString = Encoding.UTF8.GetString(ms.ToArray());
                Console.WriteLine("[" + JsonString + "]");
                Console.Error.WriteLine("flag: " + beststate.usedflag);
            }
        }
        sw1.Stop();

        Console.Error.WriteLine(sw1.ElapsedMilliseconds);
        Console.Error.WriteLine(sw2.ElapsedMilliseconds);
    }

    int[] vy = new int[] { 0, 1, 1, 0 };
    int[] vx = new int[] { -2, -1, 1, 2 };
    char[] movec = new char[] { 'p', 'a', 'l', 'b' };

    int[] vp = new int[] { 1, -1 };
    char[] rotatec = new char[] { 'd', 'k' };

    int presamestring;
    int samestring;
    StringBuilder basestring;
    int bestscore;
    string beststring;
    int beststep;


    Dictionary<string, bool>[] validdic;

    int bitCount(long x)
    {
        x = (x & 0x5555555555555555) + (x >> 1 & 0x5555555555555555);
        x = (x & 0x3333333333333333) + (x >> 2 & 0x3333333333333333);
        x = (x & 0x0f0f0f0f0f0f0f0f) + (x >> 4 & 0x0f0f0f0f0f0f0f0f);
        x = (x & 0x00ff00ff00ff00ff) + (x >> 8 & 0x00ff00ff00ff00ff);
        x = (x & 0x0000ffff0000ffff) + (x >> 16 & 0x0000ffff0000ffff);
        return (int)((x & 0x00000000ffffffff) + (x >> 32 & 0x00000000ffffffff));
    }


    int iaiaid = 0;
    int firstwidth;
    int sumwidth;

    int sumstep;

    void beamSearch()
    {
        presamestring = samestring = 0;
        basestring = new StringBuilder();
        sumwidth += firstwidth;


        //int firstwidth = 10000000 / W / H / d.sourceLength / d.sourceSeeds.Length * 500 / W / H;
        Console.Error.WriteLine("beam: " + firstwidth);

        bool lastsearch = false;
        bool lastsearchcheck = true;


        for (int t = 0; t < d.sourceLength; t++)
        {
            if (sw1.ElapsedMilliseconds > reallimit)
            {
                return;
            }
            //Console.Error.Write(t + ",");
            predic = dic;
            dic = new Dictionary<long, int>();
            int nextunit = source[t];
            var U = d.units[nextunit];
            int firsty = U.pivot.y + U.members[0].y;
            int firstx = U.pivot.x + U.members[0].x;

            char[, ,] stopmove = new char[WW, H, U.MM];
            string[, ,] stopstring = new string[WW, H, U.MM];
            int[, ,] stopbit = new int[WW, H, U.MM];


            nextsl = new List<State>();

            int beamwidth = firstwidth;

            if (d.sourceLength - t < 50) lastsearch = true;


            if (lastsearchcheck && lastsearch)
            {
                lastsearchcheck = false;
                Console.Error.WriteLine("lastsearch start at " + t + " turn");
                //lastsearch first set
                foreach (var item in nowsl)
                {
                    //item.point += 300 * bitCount(item.usedflag);
                    item.score += 300 * bitCount(item.usedflag);
                }
            }
            nowsl.Sort();

            foreach (var nowstate in nowsl)
            {
                int sumblock = 0;
                int minY = H - 1;
                for (int i = 0; i < WW; i++)
                {
                    for (int j = 0; j < H; j++)
                    {
                        if (nowstate.board[i, j] == 1)
                        {
                            minY = Math.Min(minY, j);
                            sumblock++;
                        }
                    }
                }
                if (predic[nowstate.hash] > nowstate.score) continue;

                beamwidth--;
                if (beamwidth < 0) break;

                sumstep++;

                if (beamwidth == firstwidth - 1)
                {
                    samestring = nowstate.command.Length;

                    beststate = nowstate;
                    beststep = Math.Max(t, beststep);

                    if (debug)
                    {
                        Console.Error.WriteLine(t + " " + nowstate.score + " " + nowstate.point + " " + nowsl.Count);
                        char[,] boardmemo = new char[H, WW];

                        for (int i = 0; i < H; i++)
                        {
                            for (int j = 0; j < WW; j++)
                            {
                                boardmemo[i, j] = (char)(nowstate.board[j, i] + '0');
                            }
                        }
                        for (int i = 0; i < U.members.Length; i++)
                        {
                            int x = U.pivot.x + U.members[i].x;
                            int y = U.pivot.y + U.members[i].y;
                            boardmemo[y, x] = '#';
                        }
                        if (ok(U.pivot.x, U.pivot.y) && boardmemo[U.pivot.y, U.pivot.x] == '0') boardmemo[U.pivot.y, U.pivot.x] = '@';

                        for (int i = 0; i < H; i++)
                        {
                            for (int j = 0; j < WW; j++)
                            {
                                if (i % 2 != j % 2) Console.Error.Write(' ');
                                else
                                {
                                    if (boardmemo[i, j] == '0') boardmemo[i, j] = '.';
                                    Console.Error.Write(boardmemo[i, j]);
                                }
                            }
                            Console.Error.WriteLine();
                        }

                        //Console.Error.WriteLine(firstx + " " + firsty);
                        Console.Error.WriteLine(nowstate.command);
                    }
                }
                else
                {
                    samestring = Math.Min(samestring, nowstate.command.Length);
                    for (int i = 0; i < samestring; i++)
                    {
                        if (nowsl[0].command[i] != nowstate.command[i])
                        {
                            samestring = i;
                            break;
                        }
                    }
                }

                int MM = U.MM;

                bool[, ,] find = new bool[WW, H, MM];
                bool[, ,] check = new bool[WW, H, MM];
                string[, ,] addstring = new string[WW, H, MM];
                int[, ,] nowscore = new int[WW, H, MM];

                int[, ,] stopscore = new int[WW, H, MM];
                stopmove = new char[WW, H, MM];
                stopstring = new string[WW, H, MM];
                stopbit = new int[WW, H, MM];


                int[, ,] nowstep = new int[WW, H, MM];
                int[, ,] nowbit = new int[WW, H, MM];
                
                check[firstx, firsty, 0] = true;
                addstring[firstx, firsty, 0] = "";

                int firststep = 10000;
                nowstep[firstx, firsty, 0] = firststep;
                nowscore[firstx, firsty, 0] = 0;
                nowbit[firstx, firsty, 0] = nowstate.usedflag;

                Heap<long> q = new Heap<long>();
                //cy(56) y(48) score(32) step(16) x(4) p(0)
                ////score(48) step(32) x(16) y(4) p(0)
                long firstnext = ((long)(210 - U.pivot.y) << 56) + ((long)(210 - firsty) << 48) + (firststep << 16) + (firstx << 4) + (0);
                q.push(firstnext);

                while (q.top != null)
                {
                    long nowpos = q.pop();

                    int x = (int)((nowpos >> 4) & 0xFFF);
                    int y = 210 - ((int)(nowpos >> 48) & 0xFF);
                    int p = (int)(nowpos & 0xF);
                    int step = (int)((nowpos >> 16) & 0xFFFF);
                    int sc = (int)((nowpos >> 32) & 0xFFFF);


                    //if (r.Next(1000) == 0)
                    //{
                        //Console.Error.WriteLine(x + " " + y + " " + p + " " + step + " " + sc);
                    //}

                    if (sc != nowscore[x, y, p] || step != nowstep[x, y, p]) continue;

                    int cx = x - U.allmember[0, p].x;
                    int cy = y - U.allmember[0, p].y;

                    if (!isSettable(U, nowstate, cx, cy, p))
                    {
                        //Console.Error.WriteLine("Dead!");
                        //　突然の死！！！！
                        continue;
                    }

                    //ia! ia!
                    int tx = x;
                    int ty = y;
                    int tp = p;
                    int tstep = step - 1;
                    int tsc = sc;
                    bool iaiaflag = true;

                    string iastring = "";

                    if (!lastsearchcheck && (nowbit[x, y, p] >> iaiaid) % 2 == 0) tsc += 300;

                    for(int iaia = 0; iaiaflag; iaia++)
                    {
                        int[] array = new int[] { 1, 1, 0, 2 };
                        string iast = "ia! ";
                        if (iaia == 0 && addstring[x, y, p] != null)
                        {
                            if (!validmove(ref addstring[x, y, p], iast, U.MM)) break;
                        }

                        for (int kk = 0; kk < iast.Length; kk++)
                        {
                            int k = array[kk];
                            int nx = tx + vx[k];
                            int ny = ty + vy[k];
                            int np = tp;
                            int nstep = tstep;
                            int nsc = tsc;
                            if (kk == 2 && iaia > 0) nsc += 14;


                            if (!ok(nx, ny))
                            {
                                if (kk == 2 && iaia >= 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                {
                                    stopscore[tx, ty, tp] = nsc;
                                    stopstring[tx, ty, tp] = addstring[x, y, p] + iastring;

                                    stopmove[tx, ty, tp] = iast[kk];

                                    stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaiaid);
                                }

                                iaiaflag = false;
                                break;
                            }

                            if (nowscore[nx, ny, np] >= nsc + 14)
                            {
                                iaiaflag = false;
                                break;
                            }

                            if (find[nx, ny, np])
                            {
                                if (!check[nx, ny, np])
                                {
                                    if (kk == 2 && iaia >= 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                    {
                                        stopscore[tx, ty, tp] = nsc;
                                        stopstring[tx, ty, tp] = addstring[x, y, p] + iastring;

                                        stopmove[tx, ty, tp] = iast[kk];

                                        stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaiaid);
                                    }

                                    iaiaflag = false;
                                    break;
                                }
                            }
                            else
                            {
                                find[nx, ny, np] = true;

                                int tcx = tx - U.allmember[0, p].x;
                                int tcy = ty - U.allmember[0, p].y;

                                int ncx = tcx + vx[k];
                                int ncy = tcy + vy[k];

                                if (!isSettable(U, nowstate, ncx, ncy, np))
                                {
                                    if (kk == 2 && iaia >= 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                    {
                                        stopscore[tx, ty, tp] = nsc;
                                        stopmove[tx, ty, tp] = iast[kk];
                                        stopstring[tx, ty, tp] = addstring[x, y, p] + iastring;

                                        stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaiaid);
                                    }
                                    iaiaflag = false;
                                    break;
                                }
                                check[nx, ny, np] = true;
                            }

                            iastring += iast[kk];

                            if (kk == 2 && iaia >= 1 && (nsc> nowscore[nx, ny, np] || (nsc == nowscore[nx, ny, np] && nstep > nowstep[nx, ny, np])))
                            {
                                addstring[nx, ny, np] = addstring[x,y,p] + iastring;
                                nowscore[nx, ny, np] = nsc;
                                nowstep[nx, ny, np] = nstep;

                                nowbit[nx, ny, np] = nowbit[x, y, p] | (1 << iaiaid);

                                //long next = ((long)nsc << 48) + ((long)nstep << 32) + (nx << 16) + (ny << 4) + np;
                                int ncy = ny - U.allmember[0, np].y;
                                long next = ((long)nsc << 32) + ((long)nstep << 16) + (nx << 4) + ((long)(210 - ny) << 48) + np + ((long)(210 - ncy) << 56);
                                
                                q.push(next);
                            }
                            tx = nx;
                            ty = ny;
                            tp = np;
                            tsc = nsc;
                            tstep = nstep;
                        }
                    }

                    int rplus = r.Next(phrase.Count);

                    for (int pp = 0; pp < phrase.Count; pp++)
                    {
                        int iaid = (pp + rplus) % phrase.Count;
                        string biast = phrase[iaid];
                        if (biast == "ia! ia!") continue;
                        if (!validdic[U.MM][biast]) continue;

                        tx = x;
                        ty = y;
                        tp = p;
                        tstep = step - 1;
                        tsc = sc;
                        iaiaflag = true;

                        if (!lastsearchcheck && (nowbit[x, y, p] >> iaid) % 2 == 0) tsc += 300;

                        for (int ss = Math.Min(biast.Length - 1, addstring[x, y, p].Length); ss >= 0; ss--)
                        {
                            bool skipflag = true;
                            int LL = addstring[x,y,p].Length;
                            for (int j = 0; j < ss; j++)
                            {
                                if (biast[j] != addstring[x, y, p][LL - ss + j])
                                {
                                    skipflag = false;
                                    break;
                                }
                            }
                            if (skipflag)
                            {
                                string iast = biast.Substring(ss);
                                iastring = "";
                                for (int iaia = 0; iaiaflag && iaia < 1; iaia++)
                                {
                                    if (iaia == 0 && addstring[x, y, p] != null)
                                    {
                                        if (!validmove(ref addstring[x, y, p], iast, U.MM)) break;
                                    }


                                    for (int kk = 0; kk < iast.Length; kk++)
                                    {
                                        int nowk = charToMove[iast[kk]];
                                        int tcx = tx - U.allmember[0, tp].x;
                                        int tcy = ty - U.allmember[0, tp].y;

                                        int k, nx, ny, np;
                                        if (nowk < 4)
                                        {
                                            k = nowk;
                                            nx = tx + vx[k];
                                            ny = ty + vy[k];
                                            np = tp;
                                        }
                                        else
                                        {
                                            k = nowk - 4;
                                            np = (tp + vp[k] + MM) % MM;
                                            nx = tcx + U.allmember[0, np].x;
                                            ny = tcy + U.allmember[0, np].y;
                                        }
                                        int nstep = tstep;
                                        int nsc = tsc;

                                        //Console.Error.WriteLine(kk);

                                        if (kk == iast.Length - 1) nsc += iast.Length * 2;

                                        if (!ok(nx, ny))
                                        {
                                            if (kk == iast.Length - 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                            {
                                                stopscore[tx, ty, tp] = nsc;
                                                stopstring[tx, ty, tp] = addstring[x, y, p] + iast.Substring(0, iast.Length - 1);

                                                stopmove[tx, ty, tp] = iast[kk];

                                                stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaid);
                                            }

                                            iaiaflag = false;
                                            break;
                                        }

                                        //Console.Error.WriteLine("myon");

                                        if (nowscore[nx, ny, np] >= nsc + iast.Length * 2)
                                        {
                                            iaiaflag = false;
                                            break;
                                        }


                                        if (find[nx, ny, np])
                                        {
                                            if (!check[nx, ny, np])
                                            {
                                                if (kk == iast.Length - 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                                {
                                                    stopscore[tx, ty, tp] = nsc;
                                                    stopstring[tx, ty, tp] = addstring[x, y, p] + iast.Substring(0, iast.Length - 1);

                                                    stopmove[tx, ty, tp] = iast[kk];

                                                    stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaid);
                                                }
                                                iaiaflag = false;
                                                break;
                                            }
                                        }
                                        else
                                        {
                                            find[nx, ny, np] = true;
                                            int ncx, ncy;
                                            ncx = tcx;
                                            ncy = tcy;
                                            if (nowk < 4)
                                            {
                                                ncx += vx[k];
                                                ncy += vy[k];
                                            }

                                            if (!isSettable(U, nowstate, ncx, ncy, np))
                                            {
                                                if (kk == iast.Length - 1 && nsc > nowscore[tx, ty, tp] && nsc > stopscore[tx, ty, tp])
                                                {
                                                    stopscore[tx, ty, tp] = nsc;
                                                    stopstring[tx, ty, tp] = addstring[x, y, p] + iast.Substring(0, iast.Length - 1);

                                                    stopmove[tx, ty, tp] = iast[kk];

                                                    stopbit[tx, ty, tp] = nowbit[x, y, p] | (1 << iaid);
                                                }
                                                iaiaflag = false;
                                                break;
                                            }
                                            check[nx, ny, np] = true;
                                        }

                                        if (kk == iast.Length - 1 && (nsc > nowscore[nx, ny, np] || (nsc == nowscore[nx, ny, np] && nstep > nowstep[nx, ny, np])))
                                        {
                                            iastring += iast;
                                            addstring[nx, ny, np] = addstring[x, y, p] + iastring;
                                            nowscore[nx, ny, np] = nsc;
                                            nowstep[nx, ny, np] = nstep;

                                            nowbit[nx, ny, np] = nowbit[x, y, p] | (1 << iaid);

                                            int ncy = ny - U.allmember[0, np].y;
                                            long next = ((long)nsc << 32) + ((long)nstep << 16) + (nx << 4) + ((long)(210 - ny) << 48) + np + ((long)(210 - ncy) << 56);
                                            q.push(next);
                                            //Console.Error.WriteLine(cx + " " + cy + " " + (tcx +vx[k]) + " " + (tcy+vy[k]));
                                        }
                                        tx = nx;
                                        ty = ny;
                                        tp = np;
                                        tsc = nsc;
                                        tstep = nstep;
                                    }
                                }
                            }
                        }

                        
                    }


                    int canmoveflag = 0;
                    for (int k = 0; k < 4; k++)
                    {
                        int nx = x + vx[k];
                        int ny = y + vy[k];
                        int np = p;
                        int nstep = step - 1;
                        int nsc = sc + 0;

                        


                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np])
                        {
                            if (check[nx, ny, np])
                            {
                                canmoveflag |= (1 << k);

                            }
                            else continue;
                        }
                        else
                        {
                            find[nx, ny, np] = true;

                            int ncx = cx + vx[k];
                            int ncy = cy + vy[k];

                            if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                            check[nx, ny, np] = true;
                            canmoveflag |= (1 << k);
                        }

                        if (addstring[x, y, p] != null && addstring[x, y, p].Length != 0)
                        {
                            if (!validmove(ref addstring[x, y, p], k, U.MM)) continue;
                        }

                        if (nsc > nowscore[nx, ny, np] || (nsc == nowscore[nx, ny, np] && nstep > nowstep[nx, ny, np]))
                        {
                            addstring[nx, ny, np] = addstring[x, y, p] + movec[k];
                            nowscore[nx, ny, np] = nsc;
                            nowstep[nx, ny, np] = nstep;
                            nowbit[nx, ny, np] = nowbit[x, y, p];

                            int ncy = ny - U.allmember[0, np].y;
                            long next = ((long)nsc << 32) + ((long)nstep << 16) + (nx << 4) + ((long)(210 - ny) << 48) + np + ((long)(210 - ncy) << 56);
                            q.push(next);
                        }
                    }


                    for (int k = 0; k < 2 && MM > 1; k++)
                    {
                        int np = (p + vp[k] + MM) % MM;
                        int nx = cx + U.allmember[0, np].x;
                        int ny = cy + U.allmember[0, np].y;
                        int nstep = step - 1;
                        int nsc = sc + 0;

                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np])
                        {
                            if (!check[nx, ny, np]) continue;
                        }
                        else
                        {
                            int ncx = cx;
                            int ncy = cy;

                            if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                            check[nx, ny, np] = true;
                        }

                        if (addstring[x, y, p] != null && addstring[x, y, p].Length != 0)
                        {
                            if (!validmove(ref addstring[x, y, p], k + 4, U.MM)) continue;
                        }

                        if (nsc > nowscore[nx, ny, np] || (nsc == nowscore[nx, ny, np] && nstep > nowstep[nx, ny, np]))
                        {
                            addstring[nx, ny, np] = addstring[x, y, p] + rotatec[k];
                            nowscore[nx, ny, np] = nsc;
                            nowstep[nx, ny, np] = nstep;
                            nowbit[nx, ny, np] = nowbit[x, y, p];

                            int ncy = ny - U.allmember[0, np].y;
                            long next = ((long)nsc << 32) + ((long)nstep << 16) + (nx << 4) + ((long)(210 - ny) << 48) + np + ((long)(210 - ncy) << 56);
                            q.push(next);
                        }
                    }

                    int lowy = getLowY(U, nowstate, cx, cy, p);


                    sw2.Start();

                    //Console.Error.WriteLine(lowy + " " + minY);
                    //if ((stopmove != (1 << 4) - 1) && (lowy >= minY - 1) && (nowscore[x, y, p] > nowbestscore - 400))
                    if (((canmoveflag & 6) == 0) && (lowy >= minY - 1))
                    {
                        //終了処理
                        int nextmove = 0;
                        while ((canmoveflag >> nextmove) % 2 == 1) nextmove++;
                        State nextstate = new State();
                        nextstate.board = (int[,])nowstate.board.Clone();

                        if (stopscore[x, y, p] > nowscore[x, y, p])
                        {
                            nextstate.command = nowstate.command + stopstring[x, y, p] + stopmove[x, y, p];
                            nextstate.usedflag = stopbit[x, y, p];
                            nextstate.score = stopscore[x, y, p];
                        }
                        else
                        {
                            nextstate.command = nowstate.command + addstring[x, y, p] + movec[nextmove];
                            nextstate.usedflag = nowbit[x, y, p];
                            nextstate.score = nowscore[x, y, p];
                        }

                        for (int i = 0; i < U.members.Length; i++)
                        {
                            int nx = cx + U.allmember[i, p].x;
                            int ny = cy + U.allmember[i, p].y;
                            nextstate.board[nx, ny] = 1;
                            //Console.WriteLine("put " + nx + " " + ny);
                        }

                        int ls = 0;
                        for (int i = 0; i < H; i++)
                        {
                            bool flag = true;
                            for (int j = 0; j < WW; j++)
                            {
                                if (i % 2 != j % 2) continue;
                                if (nextstate.board[j, i] != 1)
                                {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag)
                            {
                                ls++;
                                for (int j = 0; j < WW; j++)
                                {
                                    nextstate.board[j, i] = 0;
                                }
                                for (int k = i - 1; k >= 0; k--)
                                {
                                    for (int j = 0; j < WW; j++)
                                    {
                                        if (nextstate.board[j, k] == 1)
                                        {
                                            nextstate.board[j, k] = 0;
                                            nextstate.board[j ^ 1, k + 1] = 1;
                                        }
                                    }
                                }
                            }
                        }

                        if (activeunit && sumblock < WW * 3 / 2 && ls == 1 && t < d.sourceLength - 10)
                        {
                            sw2.Stop();
                            continue;
                        }

                        int addscore = 50 * ls * (ls + 1);
                        if (nowstate.old_ls >= 2)
                        {
                            addscore *= (9 + nowstate.old_ls);
                            addscore /= 10;
                        }



                        nextstate.score += nowstate.score + addscore + U.members.Length;




                        long hash = nextstate.gethash();

                        if (dic.ContainsKey(hash) && dic[hash] <= nextstate.score)
                        {
                            sw2.Stop();
                            continue;
                        }
                        dic[hash] = nextstate.score;


                        
                        //pointの評価関数をかく！！！！
                        nextstate.point = nextstate.score * 2;
                        if ((d.sourceLength - t) < 50)
                        {
                            nextstate.point *= 50;
                            nextstate.point /= (d.sourceLength - t);
                        }

                        //if (!activeunit) nextstate.point *= 10;
                        sw2.Stop();
                        for (int i = 0; i < H; i++)
                        {
                            for (int j = 0; j < WW; j++)
                            {
                                if (i % 2 != j % 2) continue;
                                if (nextstate.board[j, i] == 0)
                                {
                                    if (i != 0)
                                    {
                                        int myon = 0;
                                        for (int k = -1; k <= 1; k += 2)
                                        {
                                            int ttx = j + k;
                                            int tty = i - 1;
                                            //if (!ok(tx, ty)) continue;
                                            if (!ok(ttx, tty) || nextstate.board[ttx, tty] == 1)
                                            {
                                                nextstate.point -= (H - i) * 3;
                                            }
                                            else myon++;

                                        }
                                        if (myon == 0)
                                        {
                                            nextstate.point -= (H - i) * 3;
                                        }
                                        for (int k = -2; k <= 2; k += 4)
                                        {
                                            int ttx = j + k;
                                            int tty = i;
                                            //if (!ok(tx, ty)) continue;
                                            if (!ok(ttx, tty) || nextstate.board[ttx, tty] == 1)
                                            {
                                                nextstate.point -= (H - i) * 2;
                                            }
                                            else myon++;
                                        }

                                        if (myon == 0)
                                        {
                                            nextstate.point -= (H - i) * 2;
                                        }

                                        //nextstate.point -= (H - i) * (H - i);
                                    }
                                }
                                else
                                {
                                    //nextstate.point -= (H - i);
                                    nextstate.point -= (H - i) * (H - i);
                                    if (i <= H / 2 - 1)
                                    {
                                        lastsearch = true;
                                    }
                                }
                            }
                        }
                        sw2.Start();
                        //nextstate.point += r.Next(randnum);\
                        if (addR > 1) nextstate.point += r.Next(addR);

                        nextsl.Add(nextstate);

                        if (nextstate.score > bestscore)
                        {
                            bestscore = nextstate.score;
                            beststring = basestring + nextstate.command;
                        }
                        sw2.Stop();
                    }
                    sw2.Stop();
                }

                sw2.Stop();
            }
            nowsl = nextsl;
            if (nowsl.Count != 0 && samestring != 0)
            {
                basestring.Append(nowsl[0].command.Substring(0, samestring));
                foreach (var item in nowsl)
                {
                    item.command = item.command.Substring(samestring);
                }
                beststate.command = beststate.command.Substring(samestring);
            }
        }
    }

    Dictionary<char, int> charToMove;
    bool validmove(ref string st, int k, int MM)
    {
        if (k == 1 || k == 2) return true;
        int x = 0;
        int p = 0;
        int tx = 0;
        int tp = 0;
        if (k < 4)
        {
            tx += vx[k];
        }
        else
        {
            tp = (tp + vp[k - 4] + MM) % MM;
        }
        for (int i = st.Length - 1; i >= 0; i--)
        {
            int m = charToMove[st[i]];
            if (m == 1 || m == 2) return true;
            if (m < 4)
            {
                x -= vx[m];
            }
            else
            {
                p = (p - vp[m - 4] + MM) % MM;
            }
            if (x == tx && tp == p) return false;
        }
        return true;
    }

    bool validmove(ref string st, string iast, int MM)
    {
        int x = 0;
        int p = 0;
        int tx = 0;
        int tp = 0;
        Dictionary<int, bool> posdic = new Dictionary<int, bool>();
        bool ff = true;
        for (int i = 0; i < iast.Length; i++)
        {
            int k = charToMove[iast[i]];
            if (k == 1 || k == 2) break;
            if (k < 4) tx += vx[k];
            else tp = (tp + vp[k - 4] + MM) % MM;
            posdic[tx * 10000 + tp] = ff;
        }
        if (posdic.Count == 0) return true;

        for (int i = st.Length - 1; i >= 0; i--)
        {
            int m = charToMove[st[i]];
            if (m == 1 || m == 2) return true;
            if (m < 4)
            {
                x -= vx[m];
            }
            else
            {
                p = (p - vp[m - 4] + MM) % MM;
            }
            if (posdic.ContainsKey(x * 10000 + p)) return false;
        }
        return true;
    }

    bool validmover(ref string st, int MM)
    {
        int x = 0;
        int p = 0;
        int tx = 0;
        int tp = 1;
        int tx2 = -2;
        int tp2 = 1;
        for (int i = st.Length - 1; i >= 0; i--)
        {
            int m = charToMove[st[i]];
            if (m == 1 || m == 2) return true;
            if (m < 4)
            {
                x -= vx[m];
            }
            else
            {
                p = (p - vp[m - 4] + MM) % MM;
            }
            if (x == tx && tp == p) return false;
            if (x == tx2 && tp2 == p) return false;
        }
        return true;
    }

    int getLowY(Unit U, State nowstate, int ncx, int ncy, int np)
    {
        int ret = 0;
        for (int i = 0; i < U.members.Length; i++)
        {
            int my = U.allmember[i, np].y;
            my += ncy;
            ret = Math.Max(my, ret);
        }
        return ret;
    }

    bool isSettable(Unit U, State nowstate, int cx, int cy, int np)
    {
        for (int i = 0; i < U.members.Length; i++)
        {
            int mx = U.allmember[i, np].x;
            int my = U.allmember[i, np].y;
            mx += cx;
            my += cy;
            if (!ok(mx, my))
            {
                return false;
            }
            if (nowstate.board[mx, my] == 1)
            {
                return false;
            }
        }
        return true;
    }

    //まだ書いてないけど書かなくてもいいかも
    void submit()
    {
        hc = new HttpClient();
        var content = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            { "foo", "111" },
            { "bar", "222" },
            { "baz", "333" },
        });

        var response = hc.PostAsync("http://localhost/", content);
    }

    bool ok(int x, int y)
    {
        return y >= 0 && y < H && x >= 0 && x < WW;
    }


    class Heap<T> where T : IComparable
    {
        public HeapNode<T> top;

        public Heap() { }

        public void push(T val)
        {
            top = HeapNode<T>.meld(top, new HeapNode<T>(val));
        }

        public T pop()
        {
            T ret = top.val;
            top = HeapNode<T>.meld(top.r, top.l);
            return ret;
        }

        public void merge(Heap<T> h2)
        {
            top = HeapNode<T>.meld(top, h2.top);
        }

        public class HeapNode<NT> where NT : IComparable
        {
            public HeapNode<NT> l, r;
            public NT val;

            public HeapNode(NT _val)
            {
                val = _val;
            }

            public static HeapNode<NT> meld(HeapNode<NT> a, HeapNode<NT> b)
            {
                if (a == null) return b;
                if (b == null) return a;
                if (a.val.CompareTo(b.val) < 0)
                {
                    HeapNode<NT> temp = a;
                    a = b;
                    b = temp;
                }
                a.r = meld(a.r, b);
                HeapNode<NT> temph = a.l;
                a.l = a.r;
                a.r = temph;
                return a;
            }
        }
    }


}

