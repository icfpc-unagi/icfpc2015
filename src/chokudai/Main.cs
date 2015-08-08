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




class ICFPC2015
{
    string version = "chokudAIver0.3.2";
    bool debug = true;


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
                    if(j<6) allmember[i, j] = new Cell((px - 3 * py) / 2, (px + py) / 2);
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

        public int CompareTo(State s)
        {
            return -point.CompareTo(s.point);
        }

        public long hash;

        public long gethash()
        {
            if (hash != 0) return hash;
            hash = 1;
            for (int i = 0; i < board.GetLength(0); i++)
            {
                for (int j = 0; j < board.GetLength(1); j++)
                {
                    hash = nexthash(hash, board[i, j]);
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

    public static void Main(string[] args)
    {
        foreach (var item in args)
        {
            Console.Error.WriteLine(item);
        }
        if (args.Length >= 1)
        {
            filename = args[0];
        }
        else return;
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

    void myon()
    {
        Console.Write("[");
        bool first = true;
        string json = gettingtext();
        var serializer = new DataContractJsonSerializer(typeof(Data));
        var stream1 = new MemoryStream(Encoding.UTF8.GetBytes(json));
        d = (Data)serializer.ReadObject(stream1);
        r = new Random();
        W = d.width;
        H = d.height;
        WW = W + W;

        //d.sourceLength = 10;

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

        foreach (var seed in d.sourceSeeds)
        {
            if (first) first = false;
            else Console.Write(",");
            N = d.sourceLength;
            M = d.units.Length;
            source = new int[N];
            LCG lcg = new LCG(seed);

            for (int i = 0; i < N; i++)
            {
                source[i] = lcg.next() % M;
            }

            State firststate = new State();
            firststate.board = new int[WW, H];
            firststate.command = "";
            firststate.score = 0;
            firststate.old_ls = 0;
            
            dic = new Dictionary<long, int>();
            dic[0] = 0;
            foreach (var item in d.filled)
            {
                firststate.board[item.x, item.y] = 1;
            }
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
            Console.Write(JsonString);


        }

        Console.WriteLine("]");
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

    void beamSearch()
    {
        presamestring = samestring = 0;
        basestring = new StringBuilder();

        beststring = "";
        bestscore = 0;

        for (int t = 0; t < d.sourceLength; t++)
        {
            predic = dic;
            dic = new Dictionary<long, int>();
            int nextunit = source[t];
            var U = d.units[nextunit];
            int firsty = U.pivot.y + U.members[0].y;
            int firstx = U.pivot.x + U.members[0].x;
            
            nowsl.Sort();

            nextsl = new List<State>();
            int firstwidth = 10000000 / W / H / d.sourceLength / d.sourceSeeds.Length * 500 / W / H;
            if (firstwidth < 3) firstwidth = 3;
            int beamwidth = firstwidth;
            foreach (var nowstate in nowsl)
            {
                int minY = H - 1;
                for (int i = 0; i < WW; i++)
                {
                    for (int j = 0; j < H; j++)
                    {
                        if (nowstate.board[i, j] == 1)
                        {
                            minY = Math.Min(minY, j);
                        }
                    }
                }
                if (predic[nowstate.hash] > nowstate.score) continue;

                beamwidth--;
                if (beamwidth < 0) break;

                if (beamwidth == firstwidth - 1)
                {
                    samestring = nowstate.command.Length;

                    beststate = nowstate;
                    if (debug)
                    {
                        Console.Error.WriteLine(t + " " + nowstate.score + " " + nowstate.point);
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


                check[firstx, firsty, 0] = true;
                addstring[firstx, firsty, 0] = "";
                Queue<int> q = new Queue<int>();
                q.Enqueue((firstx << 16) + (firsty << 4) + 0);

                while (q.Count != 0)
                {
                    int nowpos = q.Dequeue();
                    int x = nowpos >> 16;
                    int y = (nowpos & 0xFFFF) >> 4;
                    int p = nowpos & 0xF;

                    int cx = x - U.allmember[0, p].x;
                    int cy = y - U.allmember[0, p].y;

                    if (!isSettable(U, nowstate, cx, cy, p))
                    {
                        //Console.Error.WriteLine("Dead!");
                        //　突然の死！！！！
                        continue;
                    }

                    int stopmove = 0;

                    for (int k = 0; k < 4; k++)
                    {
                        int nx = x + vx[k];
                        int ny = y + vy[k];
                        int np = p;


                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np])
                        {
                            if (check[nx, ny, np])
                            {
                                stopmove |= (1 << k);
                            }
                            continue;
                        }
                        find[nx, ny, np] = true;
                        
                        int ncx = cx + vx[k];
                        int ncy = cy + vy[k];

                        if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                        check[nx, ny, np] = true;

                        stopmove |= (1 << k);
                        addstring[nx, ny, np] = addstring[x, y, p] + movec[k];
                        int next = (nx << 16) + (ny << 4) + np;
                        q.Enqueue(next);
                    }

                    for (int k = 0; k < 2 && MM > 1; k++)
                    {
                        int np = (p + vp[k] + MM) % MM;
                        int nx = cx + U.allmember[0, np].x;
                        int ny = cy + U.allmember[0, np].y;

                        if (!ok(nx, ny)) continue;
                        if (find[nx, ny, np]) continue;
                        find[nx, ny, np] = true;

                        int ncx = cx;
                        int ncy = cy;

                        if (!isSettable(U, nowstate, ncx, ncy, np)) continue;
                        check[nx, ny, np] = true;
                    
                        addstring[nx, ny, np] = addstring[x, y, p] + rotatec[k];
                        int next = (nx << 16) + (ny << 4) + np;
                        q.Enqueue(next);
                    }

                    int lowy = getLowY(U, nowstate, cx, cy, p);

                    //Console.Error.WriteLine(lowy + " " + minY);
                    if (stopmove != (1 << 4) - 1 && lowy >= minY - 1)
                    {
                        //終了処理
                        int nextmove = 0;
                        while ((stopmove >> nextmove) % 2 == 1) nextmove++;
                        State nextstate = new State();
                        nextstate.board = (int[,])nowstate.board.Clone();
                        nextstate.command = nowstate.command + addstring[x, y, p] + movec[nextmove];
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

                        int addscore = 50 * ls * (ls + 1);
                        if (nowstate.old_ls >= 2)
                        {
                            addscore *= (9 + nowstate.old_ls);
                            addscore /= 10;
                        }

                        nextstate.score = nowstate.score + addscore + U.members.Length;
                        nextstate.old_ls = ls;
                        long hash = nextstate.gethash();
                        if (dic.ContainsKey(hash) && dic[hash] <= nextstate.score) continue;
                        dic[hash] = nextstate.score;
                             
                        //pointの評価関数をかく！！！！
                        nextstate.point = nextstate.score * 100;
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
                                            int tx = j + k;
                                            int ty = i - 1;
                                            //if (!ok(tx, ty)) continue;
                                            if (ok(tx, ty) && nextstate.board[tx, ty] == 1)
                                            {
                                                nextstate.point -= (H - i) * 1;
                                            }
                                            else myon++;

                                        }
                                        if (myon == 0)
                                        {
                                            nextstate.point -= (H - i) * 3;
                                        }
                                        //nextstate.point -= (H - i) * (H - i);
                                    }
                                }
                                else
                                {
                                    //nextstate.point -= (H - i);
                                    nextstate.point -= (H - i) * (H - i);
                                }
                            }
                        }
                        nextstate.point += r.Next(30);
                        nextsl.Add(nextstate);

                        if (nextstate.score > bestscore)
                        {
                            bestscore = nextstate.score;
                            beststring = basestring + nextstate.command;

                        }
                    }
                }

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

    bool isSettable(Unit U, State nowstate, int ncx, int ncy, int np)
    {
        for (int i = 0; i < U.members.Length; i++)
        {
            int mx = U.allmember[i, np].x;
            int my = U.allmember[i, np].y;
            mx += ncx;
            my += ncy;
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
                if (a.val.CompareTo(b.val) > 0)
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

